# =========================
# BUILD STAGE
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copiamos el POM padre
COPY ../pom.xml pom.xml

# Copiamos los módulos necesarios
COPY ../domain domain
COPY ../application application
COPY ../infrastructure infrastructure

# Compilamos SOLO el backend (application) desde el reactor
RUN mvn -q -pl application -am clean package -DskipTests

# =========================
# RUN STAGE
# =========================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/application/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
