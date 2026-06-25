# --- Fase 1: Compilación ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar el pom.xml para descargar las dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el archivo JAR
COPY src ./src
RUN mvn package -DskipTests

# --- Fase 2: Imagen de ejecución ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copiar el JAR generado desde la fase anterior
COPY --from=build /app/target/*.jar app.jar

# Los workers consumen colas de RabbitMQ, no necesitan exponer puertos web
ENTRYPOINT ["java", "-jar", "app.jar"]