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
RUN gradle bootJar -x test --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Crear usuario no root
RUN groupadd -r spring && useradd -r -g spring spring

# Copiar el JAR - con el nombre correcto según tu build.gradle
# archiveBaseName = 'nathbit-pos' + version = '0.0.1-SNAPSHOT'
COPY --from=build /app/build/libs/nathbit-pos-0.0.1-SNAPSHOT.jar app.jar

# Cambiar al usuario spring
USER spring:spring

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

# Exponer puerto
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]