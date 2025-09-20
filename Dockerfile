# Build stage
FROM gradle:7.6-jdk17-alpine AS build
WORKDIR /app

# Copiar archivos de configuración de Gradle primero (para cache)
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle ./gradle

# Descargar dependencias (esta capa se cachea si no cambian las dependencias)
RUN gradle dependencies --no-daemon

# Copiar el código fuente
COPY --chown=gradle:gradle src ./src

# Construir la aplicación
RUN gradle build -x test --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Crear usuario no root
RUN groupadd -r spring && useradd -r -g spring spring

# Copiar el JAR construido
COPY --from=build /app/build/libs/back-nathbit-pos-*.jar app.jar

# Cambiar al usuario spring
USER spring:spring

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Comando de inicio con opciones JVM optimizadas
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]