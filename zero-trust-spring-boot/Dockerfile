# Multi-stage build optimizado
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar JAR específico (evitar wildcards)
COPY target/zero-trust-spring-boot-1.0.0.jar app.jar

# Extraer capas del JAR
RUN java -Djarmode=layertools -jar app.jar extract

# ============================================
# STAGE 2: Runtime optimizado por capas
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Crear usuario no-root
RUN addgroup -g 1001 -S zerotrust && \
    adduser -u 1001 -S zerotrust -G zerotrust

WORKDIR /app

# Copiar capas en orden de cambio (menos → más frecuente)
COPY --from=builder --chown=zerotrust:zerotrust /app/dependencies/ ./
COPY --from=builder --chown=zerotrust:zerotrust /app/spring-boot-loader/ ./
COPY --from=builder --chown=zerotrust:zerotrust /app/snapshot-dependencies/ ./
COPY --from=builder --chown=zerotrust:zerotrust /app/application/ ./

USER zerotrust

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

EXPOSE 8080

# Usar el launcher de Spring Boot
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]