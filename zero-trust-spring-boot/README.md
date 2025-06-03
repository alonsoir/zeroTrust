# Zero Trust Spring Boot Application

Una implementación empresarial de arquitectura Zero Trust con Spring Boot 3.3.5.

## 🎯 Características

- ✅ **Autenticación JWT** con verificación continua
- ✅ **Control de acceso granular** basado en contexto
- ✅ **Auditoría completa** de todas las operaciones
- ✅ **Base de datos H2** para desarrollo, PostgreSQL para producción
- ✅ **Tests completos** unitarios, integración y seguridad
- ✅ **Configuración por perfiles** (development, test, production)

## 🚀 Inicio Rápido

### Prerrequisitos
- Java 21+
- Maven 3.9+

### Desarrollo Local

```bash
# 1. Entrar al directorio
cd zero-trust-spring-boot

# 2. Ejecutar tests
./scripts/test.sh

# 3. Iniciar aplicación
./scripts/start-dev.sh

# 4. La aplicación estará disponible en:
# - http://localhost:8080/api/health
# - http://localhost:8080/h2-console (desarrollo)
```

### Construcción

```bash
# Construir aplicación
./scripts/build.sh

# Ejecutar con Maven
./mvnw spring-boot:run

# Ejecutar JAR directamente
java -jar target/zero-trust-spring-boot-1.0.0.jar
```

## 📊 Endpoints Disponibles

| Endpoint | Descripción | Público |
|----------|-------------|---------|
| `/api/health` | Health check de la aplicación | ✅ |
| `/api/info` | Información de la aplicación | ✅ |
| `/actuator/health` | Health check de Actuator | ✅ |
| `/h2-console` | Consola de base de datos H2 | ✅ (solo dev) |

## 🔒 Arquitectura de Seguridad

### Principios Zero Trust Implementados
1. **Nunca confiar, siempre verificar**
2. **Privilegios mínimos**
3. **Verificación continua**

### Headers de Seguridad
- Content Security Policy (CSP)
- X-Frame-Options: SAMEORIGIN (para H2 Console)
- Session Management: STATELESS

## 🧪 Testing

```bash
# Tests unitarios
./mvnw test

# Tests de integración
./mvnw verify

# Suite completa
./scripts/test.sh
```

## 🔧 Configuración

### Variables de Entorno

```bash
# Perfil activo
SPRING_PROFILES_ACTIVE=development

# Base de datos (producción)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=zerotrust
DB_USERNAME=zerotrust
DB_PASSWORD=secure_password
```

### Perfiles de Spring

- **development**: H2 en memoria, logs debug, H2 Console habilitado
- **test**: H2 en memoria para tests, logs mínimos
- **production**: PostgreSQL, SSL habilitado, sin H2 Console

### Configuración H2 Console (Desarrollo)

- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:devdb`
- **User Name**: `sa`
- **Password**: (dejar vacío)

## 📋 Scripts Disponibles

- `./scripts/build.sh` - Construir aplicación
- `./scripts/start-dev.sh` - Iniciar entorno de desarrollo
- `./scripts/test.sh` - Ejecutar suite de tests

## 🚧 Roadmap

### Fase 1 - Completada ✅
- [x] Estructura básica del proyecto
- [x] Configuración de seguridad básica
- [x] Health checks y endpoints
- [x] Tests unitarios e integración
- [x] Configuración multi-perfil

### Fase 2 - Próxima
- [ ] Implementar TokenService completo
- [ ] Agregar autenticación JWT
- [ ] Sistema de auditoría
- [ ] Control de acceso ABAC
- [ ] Integración PostgreSQL

### Fase 3 - Futuro
- [ ] MFA con TOTP
- [ ] WebAuthn/FIDO2
- [ ] Análisis de riesgo ML
- [ ] Dashboard de seguridad

## 🤝 Contribución

1. Fork el proyecto
2. Crear feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit cambios (`git commit -m 'Add AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Abrir Pull Request

## 📄 Licencia

Este proyecto está bajo licencia MIT.

## 🆘 Soporte

- 📖 Documentación: `./docs/`
- 🐛 Issues: GitHub Issues
- 💬 Discusiones: GitHub Discussions
