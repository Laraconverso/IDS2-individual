FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY eCommerce/mvnw .
COPY eCommerce/.mvn .mvn
COPY eCommerce/pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY eCommerce/src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]