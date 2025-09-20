# Build stage
FROM gradle:7.6-jdk17-alpine AS build
WORKDIR /app

# Copiar todo el proyecto
COPY --chown=gradle:gradle . .

# Construir usando el wrapper de Spring Boot
RUN gradle clean bootJar --no-daemon

# Listar para debug
RUN ls -la build/libs/

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copiar el JAR ejecutable de Spring Boot (termina en .jar, no -plain.jar)
COPY --from=build /app/build/libs/*[!plain].jar app.jar

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

# Exponer puerto
EXPOSE 8080

# Usuario root por ahora para evitar problemas de permisos
USER root

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]