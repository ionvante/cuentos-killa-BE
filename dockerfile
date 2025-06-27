# ---------- build stage ----------
FROM maven:3.9.7-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero el descriptor para aprovechar caché
COPY pom.xml .

# Copiamos el código fuente
COPY src ./src

# Compilamos (omitimos tests) y generamos el .jar
RUN mvn -ntp -DskipTests package



# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiamos el artefacto resultante desde la etapa build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=prod"]
# Health check para verificar que la aplicación está corriendo
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1