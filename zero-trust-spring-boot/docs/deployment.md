# Guía de Despliegue

## Entornos

### Desarrollo Local

```bash
# Usar H2 en memoria
./scripts/start-dev.sh
```

### Staging

```bash
# Configurar variables de entorno
export SPRING_PROFILES_ACTIVE=staging
export DB_HOST=staging-postgres.example.com
export JWT_SECRET=staging-jwt-secret

# Ejecutar
./mvnw spring-boot:run
```

### Producción

```bash
# Variables de entorno requeridas
export SPRING_PROFILES_ACTIVE=production
export DB_HOST=prod-postgres.example.com
export DB_USERNAME=zerotrust_prod
export DB_PASSWORD=super-secure-password
export JWT_SECRET=production-jwt-secret-very-long
export SSL_KEYSTORE_PATH=/app/keystore.p12
export SSL_KEYSTORE_PASSWORD=keystore-password

# Construir y ejecutar
docker build -t zero-trust-app:latest .
docker run -p 8080:8080 zero-trust-app:latest
```

## Docker Compose

### Desarrollo
```bash
docker-compose up -d
```

### Producción
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Kubernetes

Ver directorio `k8s/` para manifests de Kubernetes.

## Verificación de Despliegue

```bash
# Health check
curl https://your-domain.com/api/health

# Info endpoint
curl https://your-domain.com/api/info

# Actuator health
curl https://your-domain.com/actuator/health
```
