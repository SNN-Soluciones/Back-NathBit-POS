# Build stage
FROM gradle:7.6-jdk17-alpine AS build
WORKDIR /app

# Copiar todo
COPY . .

# Construir
RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copiar el JAR con el nombre EXACTO que veo en tu imagen
COPY --from=build /app/build/libs/Back-NathBit-POS-0.0.1-SNAPSHOT.jar app.jar

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

# Exponer puerto
EXPOSE 8080

# Ejecutar
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]