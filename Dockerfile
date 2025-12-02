# Build stage
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app
COPY . .
RUN gradle bootJar -x test --no-daemon

# Runtime stage (cambié de alpine a normal)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiar JAR
COPY --from=build /app/build/libs/Back-NathBit-POS-0.0.1-SNAPSHOT.jar app.jar

# Configuración
ENV JAVA_OPTS="-Xmx512m -Xms256m"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]