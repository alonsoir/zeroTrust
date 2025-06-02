# Zero Trust Spring Boot Application

Una implementación empresarial de arquitectura Zero Trust con Spring Boot 3.3.5.

## 🎯 Características

- ✅ **Autenticación JWT** con tokens de corta duración
- ✅ **Verificación continua** de contexto y riesgo
- ✅ **Auditoría completa** de todas las operaciones
- ✅ **Control de acceso ABAC** (Attribute-Based Access Control)
- ✅ **MFA integrado** para operaciones críticas
- ✅ **Contenedores seguros** con Chainguard
- ✅ **Monitoreo completo** con métricas
- ✅ **Base de datos H2** para desarrollo, PostgreSQL para producción
- ✅ **Redis** para caché y gestión de tokens
- ✅ **Kafka** para auditoría de eventos

## 🚀 Inicio Rápido

### Prerrequisitos
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Desarrollo Local

```bash
# 1. Clonar y entrar al directorio
cd zero-trust-spring-boot

# 2. Iniciar servicios de infraestructura y aplicación
./scripts/start-dev.sh

# 3. La aplicación estará disponible en:
# - http://localhost:8080/api/health
# - http://localhost:8080/h2-console (desarrollo)
```

### Construcción

```bash
# Construir aplicación y contenedor
./scripts/build.sh

# Ejecutar tests
./scripts/test.sh

# Ejecutar con Docker Compose
docker-compose up -d

# Detener entorno
./scripts/stop-dev.sh
```

## 📊 Servicios Disponibles

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **Aplicación** | http://localhost:8080 | API principal |
| **Health Check** | http://localhost:8080/api/health | Estado de la aplicación |
| **Info** | http://localhost:8080/api/info | Información de la aplicación |
| **H2 Console** | http://localhost:8080/h2-console | Base de datos (desarrollo) |
| **Actuator** | http://localhost:8080/actuator | Métricas y monitoreo |
| **PostgreSQL** | localhost:5432 | Base de datos (producción) |
| **Redis** | localhost:6379 | Cache y tokens |
| **Kafka** | localhost:9092 | Cola de eventos |

## 🔒 Arquitectura de Seguridad

Este proyecto implementa los principios Zero Trust:

1. **Nunca confiar, siempre verificar**
2. **Privilegios mínimos**
3. **Verificación continua**
4. **Auditoría radical**

### Headers de Seguridad Implementados
- Content Security Policy (CSP)
- HTTP Strict Transport Security (HSTS)
- X-Frame-Options
- X-Content-Type-Options

## 🧪 Testing

```bash
# Tests unitarios
./mvnw test

# Tests de integración
./mvnw verify

# Suite completa de tests
./scripts/test.sh

# Tests con cobertura
./mvnw test jacoco:report
```

## 📈 Endpoints de Monitoreo

- **Health Check**: `/api/health`
- **Info**: `/api/info`
- **Actuator Health**: `/actuator/health`
- **Métricas**: `/actuator/metrics`

## 🔧 Configuración

### Variables de Entorno

```bash
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=zerotrust
DB_USERNAME=zerotrust
DB_PASSWORD=secure_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# JWT
JWT_SECRET=your-super-secure-jwt-secret

# Kafka
KAFKA_SERVERS=localhost:9092
```

### Perfiles de Spring

- **development**: H2 en memoria, logs debug
- **test**: H2 en memoria, sin Flyway
- **production**: PostgreSQL, SSL habilitado

### Configuración H2 Console (Desarrollo)

- **JDBC URL**: `jdbc:h2:mem:devdb`
- **User Name**: `sa`
- **Password**: (dejar vacío)

## 🐳 Docker

```bash
# Construir imagen
docker build -t zero-trust-app:latest .

# Ejecutar con Docker Compose
docker-compose up -d

# Ver logs
docker-compose logs -f zero-trust-app

# Detener todo
docker-compose down
```

## 📋 Scripts Disponibles

- `./scripts/build.sh` - Construir aplicación y contenedor
- `./scripts/start-dev.sh` - Iniciar entorno de desarrollo
- `./scripts/stop-dev.sh` - Detener entorno
- `./scripts/test.sh` - Ejecutar suite de tests

## 🚧 Roadmap

### Fase 1 - Completada ✅
- [x] Estructura básica del proyecto
- [x] Configuración de seguridad
- [x] Health checks y endpoints básicos
- [x] Tests unitarios e integración
- [x] Docker y docker-compose

### Fase 2 - Próxima
- [ ] Implementar TokenService completo
- [ ] Agregar MFA con TOTP
- [ ] Implementar ABAC PolicyEngine
- [ ] Sistema de auditoría completo
- [ ] Dashboard de seguridad

### Fase 3 - Futuro
- [ ] Integrar WebAuthn/FIDO2
- [ ] Análisis de comportamiento ML
- [ ] API Gateway integration
- [ ] Microservices support

## 🤝 Contribución

1. Fork el proyecto
2. Crear feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit cambios (`git commit -m 'Add AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Abrir Pull Request

## 📄 Licencia

Este proyecto está bajo licencia MIT. Ver `LICENSE` para más detalles.

## 🆘 Soporte

- 📖 Documentación: `./docs/`
- 🐛 Issues: GitHub Issues
- 💬 Discusiones: GitHub Discussions
- 📧 Email: security-team@company.com
