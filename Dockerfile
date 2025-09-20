# Build stage
FROM gradle:7.6-jdk17-alpine AS build
WORKDIR /app

# Copiar todo
COPY . .

# Construir
RUN gradle clean bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copiar el JAR con nombre fijo
COPY --from=build /app/build/libs/app.jar app.jar

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

# Exponer puerto
EXPOSE 8080

# Ejecutar
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]