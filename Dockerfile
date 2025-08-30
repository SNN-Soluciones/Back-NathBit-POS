# Build stage
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app

# Copiar archivos de gradle
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Descargar dependencias (se cachea si no cambian)
RUN gradle dependencies --no-daemon

# Copiar código fuente
COPY src ./src

# Construir aplicación
RUN gradle bootJar --no-daemon

# Runtime stage
FROM openjdk:17-alpine
WORKDIR /app

# Instalar wget para healthcheck
RUN apk add --no-cache wget

# Crear usuario no-root para seguridad
RUN addgroup -g 1000 nathbit && \
    adduser -D -u 1000 -G nathbit nathbit

# Copiar jar desde build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Cambiar permisos
RUN chown -R nathbit:nathbit /app

# Cambiar a usuario no-root
USER nathbit

# Puerto por defecto de Spring Boot
EXPOSE 8080

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

# Healthcheck simple
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Ejecutar aplicación con variables de entorno
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]