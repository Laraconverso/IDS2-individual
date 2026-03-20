# 20261C - Individual: Ingeniería de Software II

## ECommerce API - REST Backend Service

## Tabla de Contenido

- [Introducción](#introducción)
- [Arquitectura y Decisiones Técnicas](#arquitectura-y-decisiones-técnicas)
- [Desafíos del Proyecto](#desafíos-del-proyecto)
- [Pre-requisitos](#pre-requisitos)
- [Lenguaje y Tecnologías Utilizadas](#lenguaje-y-tecnologías-utilizadas)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Inicio Rápido con Makefile](#inicio-rápido-con-makefile)
- [Comandos para Construir la Imagen Docker](#comandos-para-construir-la-imagen-docker)
- [Comandos para Ejecutar la Base de Datos](#comandos-para-ejecutar-la-base-de-datos)
- [Comandos para Ejecutar el Servicio](#comandos-para-ejecutar-el-servicio)
- [Testing](#testing)
- [Mejoras a la Solución](#mejoras-a-la-solución)
- [CI/CD y GitHub Actions](#cicd-y-github-actions)

## Introducción

El proyecto consta de la implementación de un servicio de backend para un eCommerce. El servicio expone una API REST que gestiona productos y carrito de compras, permitiendo operaciones CRUD sobre ambas entidades, con énfasis en la correcta gestión de transacciones, persistencia de datos y manejo de errores siguiendo estándares de la industria.

## Arquitectura y Decisiones Técnicas

### Estructura de Capas

La arquitectura implementada sigue un patrón de capas, organizando el proyecto en 4 niveles:
1. **Controllers** - Capa de presentación que expone los endpoints REST
2. **Services** - Capa de lógica de negocio
3. **Repositories** - Capa de acceso a datos
4. **DTOs/Entities** - Modelos de datos

El mapeo objeto relacional se realizó mediante **JPA/Hibernate**, empleando **PostgreSQL** como base de datos relacional. El manejo de errores se implementó mediante un middleware global que sigue el estándar **RFC 7807** (Problem Details for HTTP APIs), proporcionando respuestas de error consistentes y estructuradas en formato JSON.

El sistema puede ser **orquestado completamente con Docker Compose**, permitiendo levantar tanto la base de datos PostgreSQL como el servicio backend en contenedores aislados, facilitando la reproducibilidad del entorno sin depender de instalaciones locales de JDK, Maven o PostgreSQL.


### Decisiones Técnicas Clave

Para armar este backend, intenté priorizar mantener el código limpio y separar bien las responsabilidades. Estas fueron las herramientas y patrones que decidí implementar:

* **ORM y Repositorios (Spring Data JPA / Hibernate):** Decidí no ensuciar el código con SQL nativo. Usar JPA me permitió manejar el ciclo de vida de los objetos y aprovechar las validaciones de Jakarta (`@NotNull`, `@DecimalMin`) directo en las entidades. Además, al extender `JpaRepository`, me ahorré armar el CRUD básico a mano y pude resolver consultas complejas simplemente nombrando bien los métodos (como `findByUserIdOrderByAddedAtDescIdDesc`).
* **Separación estricta con DTOs:** Me pareció fundamental no exponer las entidades de la base de datos directamente en los endpoints. Usar DTOs me dio el control total de qué datos entran y salen, permitiéndome validar los *requests* con `@Valid` en la capa de la API antes de que toquen la lógica de negocio.
* **Manejo de Errores (RFC 7807):** Quería que la API fuera predecible. Por eso, en lugar de devolver errores genéricos o *stacktraces* horribles, armé un manejador global de excepciones que ataja cualquier problema y lo formatea siguiendo el estándar RFC 7807, devolviendo siempre un JSON estructurado y claro.
* **Transaccionalidad (`@Transactional`):** Para proteger la integridad de la base de datos, manejé las transacciones a nivel de servicio. Marqué las consultas de solo lectura con `readOnly = true` para optimizar el rendimiento, y dejé el manejo de operaciones complejas (como borrar un producto y sus ítems del carrito) bajo transacciones atómicas para que Spring haga un *rollback* automático si algo falla.
* **Trazabilidad con Logs:** Agregué SLF4J a través de las anotaciones de Lombok (`@Slf4j`). Tener logs estructurados de lo que pasa en los endpoints me resultó indispensable para poder debuggear el código rápido sin tener que frenar la ejecución a cada rato.

## Desafíos del Proyecto

Este proyecto fue un desafío, durante el desarrollo, me encontré con varias decisiones arquitectónicas clave:

**El diseño del Carrito de Compras**
Fue el punto que más tiempo de análisis me llevó. Al principio, la forma más intuitiva parecía ser relacionar directamente el ítem del carrito con la tabla de productos. Sin embargo, me di cuenta de que si el precio de un producto cambiaba en el catálogo, automáticamente le alteraría el total al usuario que ya lo tenía en su carrito. Para evitar esto, implementé un modelo de *Snapshot*: al momento de agregar un ítem, el sistema hace una copia estática del título y el precio. Así, el carrito mantiene su integridad histórica, tal como operan los e-commerce reales.

**Manejo centralizado de Errores (RFC 7807)**
Al tener la lógica dividida en múltiples capas, las excepciones podían saltar en cualquier lado y llegar al cliente con formatos inconsistentes. Para estandarizar esto e implementar el RFC 7807 exigido, centralicé la captura de excepciones utilizando un `@RestControllerAdvice`. Esto me permitió atrapar cualquier fallo (desde un producto no encontrado hasta un error de validación) y transformarlo en una respuesta JSON limpia, predecible y segura, sin exponer detalles internos del servidor.

**Mapeo de Datos y Transaccionalidad**
Otro reto importante fue evitar la repetición de código y mantener la limpieza al convertir Entidades a DTOs. Lo resolví aislando esa lógica en métodos de mapeo dedicados dentro de los servicios. Por otro lado, operaciones complejas como eliminar un producto (que obliga a limpiar en cascada los ítems de los carritos afectados) me exigieron manejar la transaccionalidad de forma explícita. Usar `@Transactional` fue fundamental para garantizar que estas operaciones fueran atómicas: si algo falla en el medio del proceso, la base de datos hace un *rollback* completo y evita quedar en un estado inconsistente.

## Pre-requisitos

Para levantar el entorno de desarrollo, hay dos opciones:

### Opción 1: Manual 
Requiere instalar en la máquina local:
- **JDK 25+** (Java Development Kit)
- **Apache Maven 3.6+** (Build tool)
- **PostgreSQL 15+** (Base de datos)

### Opción 2: Con Docker 
Solo requiere:
- **Docker** (v20.10+)
- **Docker Compose** (v2.0+)


**Verificar instalación**:
```bash
docker --version
docker compose version
```

### Configuración de Variables de Entorno

El proyecto utiliza un archivo `.env` en la raíz para centralizar la configuración de todas las variables de entorno.

#### Clonar archivo de ejemplo 
```bash
cp .env.example .env
```

## Lenguaje y Tecnologías Utilizadas

### Java y Spring Framework
El proyecto está desarrollado en **Java 25** usando **Spring Boot 4.0.3** con las siguientes dependencias clave:

| Dependencia | Propósito | Versión |
|-------------|-----------|---------|
| `spring-boot-starter-data-jpa` | ORM con Hibernate | 4.0.3 |
| `spring-boot-starter-webmvc` | REST API y servlet | 4.0.3 |
| `spring-boot-starter-validation` | Validación de entrada | 4.0.3 |
| `postgresql` | Driver JDBC para PostgreSQL | Latest |
| `lombok` | Generación de código boilerplate | Latest |

## Inicio Rápido con Makefile

El proyecto incluye un `Makefile` que facilita la ejecución de comandos comunes. 

### Comandos Disponibles

#### **Inicio Rápido** (Lo más común)
```bash
# Levantar TODO (DB + API)
make up

# O en background
make up-detached
```

#### **Monitoreo**
```bash
# Ver logs de todos los servicios
make logs

# Ver solo logs de la API
make logs-app

# Ver solo logs de la BD
make logs-db
```

#### **Testing**
```bash
# Ejecutar tests localmente (requiere JDK 25)
make test

# Ejecutar tests dentro de Docker
make test-docker

# Limpiar y ejecutar tests
make test-clean
```

#### **Construcción**
```bash
# Construir imagen Docker
make build

# Empaquetar JAR
make maven-package

# Compilar código
make maven-compile
```

#### **Control de Servicios**
```bash
# Detener servicios (sin eliminar datos)
make stop

# Reiniciar servicios
make restart

# Ver estado de contenedores
make ps
make status
```

#### **Limpieza**
```bash
# Eliminar contenedores, volúmenes e imágenes
make clean

# Limpiar solo artefactos Maven
make maven-clean

# Eliminar todo incluyendo datos de BD
make down-volumes
```

#### **Información**
```bash
# Ver versiones de Java y Maven
make version

# Ver estado del sistema
make status
```

#### **Ayuda**
```bash
# Ver todos los comandos disponibles
make help
```

### Ejemplo de Flujo de Trabajo Típico

```bash
# 1. Clonar el repositorio
git clone <url-del-repo>
cd IDS2-individual

# 2. Levantar la aplicación
make up

# 3. En otra terminal, ver logs
make logs

# 4. Cuando hayas terminado, detener
make down
```

### Equivalencia de Comandos

| Makefile | Docker Compose Equivalente |
|----------|----------------------------|
| `make up` | `docker compose up --build` |
| `make up-detached` | `docker compose up --build -d` |
| `make down` | `docker compose down` |
| `make logs` | `docker compose logs -f` |
| `make stop` | `docker compose stop` |
| `make restart` | `docker compose restart` |

### Requisitos para Usar Makefile

- Descargar Makefile

---

## Comandos para Construir la Imagen Docker

### Construcción Automática (Recomendado)
Docker Compose construye automáticamente la imagen al ejecutar `up --build`. Sin embargo, si necesitas construirla explícitamente:

```bash
docker compose build
```

### Construcción Manual del Dockerfile
```bash
docker build -t ecommerce-api:latest .
```

**Nota**: El `Dockerfile` implementa un **multi-stage build** que:
1. **Stage 1 (Builder)**: Compila el código Java usando Maven
2. **Stage 2 (Runtime)**: Copia solo el JAR compilado a una imagen JRE más pequeña

Esto reduce el tamaño final de la imagen (JRE vs JDK) y mejora performance en producción.

## Comandos para Ejecutar la Base de Datos

### Opción 1: Con Docker Compose (Todo Junto)
```bash
docker compose up --build
```
Esto levanta tanto la BD como el servicio automáticamente.

### Opción 2: Solo Base de Datos
```bash
docker compose up --build db
```
Levanta solo PostgreSQL en puerto `5432`. La aplicación puede conectarse desde localhost.

## Comandos para Ejecutar el Servicio

### Opción 1: Construcción y Ejecución en Un Paso
```bash
docker compose up --build
```
Levanta todo el stack (DB + API) con las siguientes configuraciones:
- **API**: Puerto `8080`
- **DB**: Puerto `5432`
- **Ambiente**: `development`

### Opción 2: Separar Construcción y Ejecución
```bash
# Construcción
docker compose build

# Ejecución
docker compose up
```

## Testing

### Framework y Herramientas
- **Spring Boot Test** para contexto completo de la aplicación
- **MockMvc** para realizar peticiones HTTP reales en tests
- **H2 Database** para tests aislados en memoria, los usé para testear localmente
- **JUnit 5** (incluido en Spring Boot Test)

### Enlaces de Referencia (User Guides)
- [Guía de Usuario de JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [Documentación oficial de Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

### Ejecución de Tests

Local con Maven:
```bash
cd eCommerce
./mvnw test
```

Test específico:
```bash
./mvnw test -Dtest=CartControllerTest#addItemToCart_Success
```
## Mejoras a la Solución

Hay algunas cosas que me gustaría sumarle a la arquitectura para acercarla a un entorno productivo un poco mas real:
* Paginación y Filtros: Actualmente, pedir la lista de productos trae todos los registros de golpe. A medida que el catálogo crezca, esto va a traer problemas de memoria y red. Me gustaría agregarle paginación y parámetros en la URL para poder buscar por texto o filtrar por rango de precio directamente desde la base de datos.
* Historial de Órdenes (Checkout): Hoy el carrito es completamente efímero. El paso lógico sería crear una entidad `Order` (Orden de Compra) para que, al momento de "pagar", ese carrito se convierta en un registro histórico con estados fijos (Pendiente, Pagada, Cancelada) y le quede guardado en el perfil al usuario.
* Seguridad y Roles: El sistema ahora asume que el cliente que consume la API tiene permiso para todo. Faltaría integrar Spring Security con tokens JWT para diferenciar roles: que los usuarios normales solo puedan gestionar su propio carrito, y que exista un rol de vendedor/administrador exclusivo para crear o borrar productos.
* Performance y Monitoreo: Para optimizar las consultas, sumaría Redis como caché para el catálogo de productos, ya que es información de alta lectura y baja modificación. Por último, me encantaría exponer las métricas del negocio usando Actuator para armar un buen dashboard visual en Grafana.

## CI/CD y GitHub Actions

El proyecto incluye un pipeline de Integración Continua automatizado mediante **GitHub Actions** (`.github/workflows/test.yml`).
