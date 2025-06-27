# ---------- build stage ----------
FROM maven:3.9.7-eclipse-temurin-21 AS build   # JDK 21 + Maven
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -ntp -DskipTests package                # genera el .jar

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre                    # JRE 21 slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=prod"]