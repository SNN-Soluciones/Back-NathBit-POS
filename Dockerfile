# Build stage
FROM gradle:7.6-jdk17-alpine AS build
WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle ./gradle

# Descargar dependencias
RUN gradle dependencies --no-daemon || true

# Copiar el código fuente
COPY --chown=gradle:gradle src ./src

# Construir la aplicación
RUN gradle build -x test --no-daemon

# Verificar qué archivos se generaron (para debug)
RUN ls -la /app/build/libs/

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Crear usuario no root
RUN groupadd -r spring && useradd -r -g spring spring

# Copiar TODOS los JARs y renombrar (más seguro)
COPY --from=build /app/build/libs/*.jar app.jar

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

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]