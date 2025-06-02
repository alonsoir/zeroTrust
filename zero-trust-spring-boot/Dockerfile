# Multi-stage Dockerfile para Zero Trust Application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar archivos de configuración Maven
COPY pom.xml ./
COPY .mvn .mvn/
COPY mvnw ./

# Descargar dependencias
RUN ./mvnw dependency:go-offline -B || echo "Some dependencies failed, continuing..."

# Copiar código fuente
COPY src ./src/

# Compilar aplicación
RUN ./mvnw clean package -DskipTests
RUN sha256sum target/*.jar > target/app.jar.sha256

# Imagen final optimizada
FROM cgr.dev/chainguard/jre:latest

LABEL maintainer="security-team@company.com" \
      version="1.0.0" \
      description="Zero Trust Spring Boot Application"

ENV SPRING_PROFILES_ACTIVE=production \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

USER nonroot
WORKDIR /app

# Copiar JAR desde builder
COPY --from=builder --chown=nonroot:nonroot /app/target/*.jar app.jar
COPY --from=builder --chown=nonroot:nonroot /app/target/app.jar.sha256 app.jar.sha256

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
