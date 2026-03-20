FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Le agregamos la carpeta eCommerce/ a las rutas
COPY eCommerce/mvnw .
COPY eCommerce/.mvn .mvn
COPY eCommerce/pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Lo mismo acá
COPY eCommerce/src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

ENV ENVIRONMENT=development
ENV HOST=0.0.0.0
ENV PORT=8080

COPY --from=builder /app/target/*.jar app.jar
EXPOSE ${PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]