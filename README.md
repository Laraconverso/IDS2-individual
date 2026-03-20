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

El presente proyecto consta de la implementación de un servicio de backend para un eCommerce. El servicio expone una API REST que gestiona productos y carrito de compras, permitiendo operaciones CRUD sobre ambas entidades, con énfasis en la correcta gestión de transacciones, persistencia de datos y manejo de errores siguiendo estándares de la industria.

## Arquitectura y Decisiones Técnicas

### Estructura de Capas

La arquitectura implementada sigue un patrón de capas, organizando el proyecto en 4 niveles:
1. **Controllers** - Capa de presentación que expone los endpoints REST
2. **Services** - Capa de lógica de negocio
3. **Repositories** - Capa de acceso a datos
4. **DTOs/Entities** - Modelos de datos

El mapeo objeto-relacional se realizó mediante **JPA/Hibernate**, empleando **PostgreSQL** como base de datos relacional. El manejo de errores se implementó mediante un middleware global que sigue el estándar **RFC 7807** (Problem Details for HTTP APIs), proporcionando respuestas de error consistentes y estructuradas en formato JSON.

El sistema puede ser **orquestado completamente con Docker Compose**, permitiendo levantar tanto la base de datos PostgreSQL como el servicio backend en contenedores aislados, facilitando la reproducibilidad del entorno sin depender de instalaciones locales de JDK, Maven o PostgreSQL.


### Decisiones Técnicas Clave

#### 1. **JPA/Hibernate para ORM**
Se eligió JPA/Hibernate como framework de mapeo objeto-relacional porque:
- Abstrae la complejidad del mapeo entre objetos Java y tablas SQL
- Maneja automáticamente transacciones y ciclo de vida de objetos
- Proporciona validación a través de anotaciones (`@NotNull`, `@NotBlank`, `@DecimalMin`)
- Integración nativa con Spring Data JPA para acceso a datos

#### 2. **Patrón Repository**
Se implementó el patrón Repository mediante `JpaRepository` para:
- Desacoplar la lógica de negocio del acceso a datos
- Reutilizar operaciones CRUD estándar
- Definir consultas personalizadas (custom finder methods) como `findByUserIdOrderByAddedAtDescIdDesc()`

#### 3. **DTOs (Data Transfer Objects)**
Separación clara entre modelos internos (Entities) y representación externa (DTOs) para:
- Evitar exposición de detalles internos de persistencia
- Validación de entrada en el nivel de API (`@Valid`)
- Transformación de datos sin impactar lógica de negocio
- Control sobre qué campos se serializan en respuestas JSON

#### 4. **RFC 7807 - Problem Details for HTTP APIs**
Se implementó un manejador global de excepciones siguiendo RFC 7807 para:
- Respuestas de error estructuradas y consistentes
- Información clara y contextualizada en errores
- Compatible con clientes que esperan este formato estándar de la industria
- Mejora significativa en la experiencia del desarrollador (Developer Experience)

#### 5. **Transacciones Explícitas**
Se utilizó `@Transactional` en servicios para:
- Garantizar atomicidad en operaciones complejas
- `readOnly = true` en consultas para optimización de base de datos
- Rollback automático ante excepciones

#### 6. **Logging Estructurado**
Se agregó logging con **SLF4J** a través de `@Slf4j` de Lombok para:
- Rastreo de operaciones en tiempo real
- Debugging y monitoreo en producción
- Contexto claro de qué ocurre en cada endpoint

#### 7. **Snapshots en Carrito**
El carrito almacena un "snapshot" (fotografía) del estado del producto en el momento de la compra:
- `title` y `price` se duplican en `CartItem`
- Permite cambiar producto sin afectar historial de carrito
- Reflection del patrón real en sistemas de e-commerce

### Flujo de una Petición HTTP

```
HTTP Request
    ↓
CartController / ProductController
    ↓
@Valid Validation (DTOs)
    ↓
Service Layer (Lógica de Negocio)
    ↓
Repository Layer (JPA Queries)
    ↓
PostgreSQL Database
    ↓
Mapping a DTO
    ↓
ResponseWrapper<T>
    ↓
HTTP Response (JSON)
```

## Desafíos del Proyecto

Este proyecto funcionó como un primer acercamiento formal a una arquitectura de microservicios con buenas prácticas de desarrollo profesional. La principal motivación fue no solo cumplir requisitos funcionales, sino internalizarizarlos mediante experiencia práctica con tecnologías de uso industrial.

### Desafío 1: Patrón Snapshot en Carrito
**Problema**: Decidir cómo manejar cambios de precios en productos cuando ya están en el carrito.

**Solución**: Se implementó un patrón de snapshot donde `CartItem` guarda copias del `title` y `price` en el momento en que se agrega el producto. Esto asegura que:
- El carrito refleja exactamente el estado en el que se agregó el producto
- Cambios posteriores en el producto original no afectan items existentes en carritos
- Es el patrón usado en sistemas reales de e-commerce

### Desafío 2: Gestión de Errores Escalable
**Problema**: Diferentes excepciones en diferentes capas, sin forma consistente de representarlas al cliente.

**Solución**: Se implementó un `@RestControllerAdvice` global que:
- Captura excepciones en toda la aplicación
- Las traduce a respuestas HTTP con formato RFC 7807
- Proporciona contexto claro del error sin exponer detalles internos
- Es extensible para agregar más tipos de excepciones en el futuro

### Desafío 3: Mapeo de Entidades a DTOs
**Problema**: Repetición de código al convertir entre `Entity` → `DTO` en cada servicio.

**Solución**: Se implementó un patrón private helper method `mapToDto()` en cada servicio que:
- Centraliza la lógica de transformación
- Es reutilizable en múltiples métodos
- Facilita refactoring futuro si la estructura de DTO cambia

### Desafío 4: Transaccionalidad y Consistencia
**Problema**: Asegurar que operaciones complejas (agregar a carrito, eliminar producto) sean atómicas.

**Solución**: Se utilizó `@Transactional` junto con `saveAndFlush()` cuando es necesario para:
- Garantizar que todos los cambios se persistan juntos
- Realizar rollback automático si algo falla
- Evitar estados inconsistentes en la base de datos

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

Esta opción es **altamente recomendada** porque:
- No requiere instalaciones adicionales
- Garantiza consistencia entre ambientes
- Aísla dependencias en contenedores
- Simula más fielmente un entorno de producción

**Verificar instalación**:
```bash
docker --version
docker compose version
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

El proyecto incluye un `Makefile` que facilita la ejecución de comandos comunes. Esta es la forma más recomendada y sencilla de trabajar con el proyecto.

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

#### 🗑️ **Limpieza**
```bash
# Eliminar contenedores, volúmenes e imágenes
make clean

# Limpiar solo artefactos Maven
make maven-clean

# Eliminar todo incluyendo datos de BD
make down-volumes
```

#### ℹ️ **Información**
```bash
# Ver versiones de Java y Maven
make version

# Ver estado del sistema
make status
```

#### 📋 **Ayuda**
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

Hay varios aspectos que podrían mejorarse en versiones futuras:

### 1. **Filtros y Búsqueda en Productos**
Actualmente, `getAllProducts()` retorna todos los productos. Una mejora sería:
- Agregar búsqueda por nombre/descripción
- Nuevos filtros por ejemplo: filtrado por rango de precio

### 2. **Persistencia de Órdenes de Compra**
Actualmente, el carrito es efímero. Una mejora sería:
- Entity `Order` que guarda snapshot del carrito en un momento
- Historial de compras por usuario
- Estados de orden (pendiente, completada, cancelada)

### 3. **Autenticación y Autorización**
El proyecto no incluye seguridad. Se podria agregar una capa de seguridad usando:
- Spring Security con JWT tokens
- Separación de roles (admin, customer)

### 4. **Caching**
Para mejorar performance:
- Spring Cache con Redis para productos consultados frecuentemente

### 5. **Métricas y Monitoreo**
- Exponer métricas de business (productos creados, carritos llenos, etc.)
- Integración con Grafana

## CI/CD y GitHub Actions

El proyecto incluye un pipeline de Integración Continua automatizado mediante **GitHub Actions** (`.github/workflows/test.yml`).