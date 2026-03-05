# ══════════════════════════════════════════
#   Build Stage — compila el JAR con Maven
# ══════════════════════════════════════════
FROM maven:3.9.7-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copiar primero el pom para aprovechar cache de dependencias
COPY pom.xml .
RUN mvn -ntp dependency:go-offline -q

# Copiar código fuente y compilar
COPY src ./src
RUN mvn -ntp -DskipTests package -q

# ══════════════════════════════════════════
#   Runtime Stage — imagen mínima con JRE
# ══════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Crear directorio para uploads con permisos correctos
RUN mkdir -p uploads/vouchers && chown -R spring:spring uploads

# Copiar el JAR resultante
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring

# Puerto que Railway asignará vía $PORT
EXPOSE 8080

# Opciones JVM optimizadas para contenedores con poca RAM (Railway free: ~512 MB)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]