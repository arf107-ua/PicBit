# --- Fase 1: Compilación ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests

# --- Fase 2: Imagen de ejecución ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# El puerto de la API web
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]