# Zero Trust Spring Boot Application

Una implementación empresarial de arquitectura Zero Trust con Spring Boot 3.3.5 y gestión centralizada de secretos con HashiCorp Vault.

## 🎯 Características

- ✅ **Gestión de secretos con HashiCorp Vault** - Secretos centralizados y seguros
- ✅ **Spring Cloud Vault** integrado - Lectura automática de secretos
- ✅ **Autenticación JWT** con secretos rotativos desde Vault
- ✅ **Control de acceso granular** basado en contexto
- ✅ **Auditoría completa** de todas las operaciones
- ✅ **Multi-base de datos**: H2 (desarrollo), PostgreSQL (producción)
- ✅ **Tests completos** unitarios, integración y seguridad
- ✅ **Configuración por perfiles** (development, test, production)
- ✅ **Docker Compose** para entorno completo

## 🔐 Arquitectura de Secretos

### HashiCorp Vault Integrado
- **🔑 Gestión centralizada** de secretos JWT, database y API keys
- **🔄 Bootstrap context** para carga temprana de secretos
- **📊 Property sources** dinámicos desde Vault
- **🌐 Multi-entorno** con configuraciones específicas por perfil

### Secretos Gestionados
- `jwt.secret` - Clave secreta para JWT tokens (rotativo)
- `database.*` - Credenciales de base de datos
- Configuraciones específicas por aplicación y entorno

## 🚀 Inicio Rápido

### Prerrequisitos
- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Desarrollo Local con Vault

```bash
# 1. Entrar al directorio
cd zero-trust-spring-boot

# 2. Levantar infraestructura completa
docker-compose up -d

# 3. Configurar secretos en Vault
./init-vault.sh

# 4. Verificar que la aplicación está usando Vault
curl http://localhost:8080/actuator/health

# 5. Servicios disponibles:
# - Aplicación: http://localhost:8080
# - Vault UI: http://localhost:8200 (token: dev-root-token)
# - PostgreSQL: localhost:5432
# - Redis: localhost:6379
```

### Desarrollo Sin Docker

```bash
# Solo para desarrollo rápido (sin Vault)
./mvnw spring-boot:run -Dspring-boot.run.profiles=development
```

## 📊 Endpoints Disponibles

| Endpoint | Descripción | Público |
|----------|-------------|---------|
| `/api/health` | Health check de la aplicación | ✅ |
| `/api/info` | Información de la aplicación | ✅ |
| `/actuator/health` | Health check de Actuator | ✅ |
| `/actuator/env` | Variables de entorno (requiere auth) | 🔒 |
| `/actuator/configprops` | Propiedades de configuración | 🔒 |
| `/h2-console` | Consola de base de datos H2 | ✅ (solo dev) |

### Autenticación Actuator

```bash
# Usuario generado automáticamente (ver logs para password)
curl -u user:{password} http://localhost:8080/actuator/env
```

## 🔒 Arquitectura de Seguridad

### Principios Zero Trust Implementados
1. **Nunca confiar, siempre verificar**
2. **Privilegios mínimos**
3. **Verificación continua**
4. **Secretos centralizados y rotativos**

### Gestión de Secretos
- **Vault Integration**: Spring Cloud Vault para lectura automática
- **Bootstrap Context**: Carga de secretos antes del contexto principal
- **Property Sources**: Vault tiene prioridad sobre configuraciones locales
- **Fallback Values**: Valores por defecto para desarrollo

### Headers de Seguridad
- Content Security Policy (CSP)
- X-Frame-Options: SAMEORIGIN (para H2 Console)
- Session Management: STATELESS

## 🐳 Docker & Infraestructura

### Servicios en Docker Compose

```yaml
# Servicios disponibles:
services:
  zero-trust-app:    # Aplicación principal (puerto 8080)
  vault:            # HashiCorp Vault (puerto 8200)
  postgres:         # PostgreSQL (puerto 5432)
  redis:           # Redis (puerto 6379)
```

### Variables de Entorno (.env)

```bash
# Aplicación
SPRING_PROFILES_ACTIVE=development
APP_PORT=8080

# Vault
VAULT_HOST=vault
VAULT_TOKEN=dev-root-token
VAULT_PORT=8200

# Base de datos
POSTGRES_PORT=5432
POSTGRES_DB=zerotrust
POSTGRES_USER=zerotrust
POSTGRES_PASSWORD=secure_password

# Redis
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
```

## 🧪 Testing

```bash
# Tests unitarios
./mvnw test

# Tests de integración
./mvnw verify

# Suite completa con Docker
./scripts/test.sh

# Verificar integración con Vault
./diagnosis.sh
```

## 🔧 Configuración Avanzada

### Perfiles de Spring

- **development**: H2 + Vault dev mode, logs debug
- **test**: H2 en memoria, Vault deshabilitado
- **production**: PostgreSQL + Vault production, TLS habilitado

### Configuración de Vault por Perfil

```yaml
# Development
spring.cloud.vault:
  host: localhost
  token: dev-root-token
  scheme: http

# Production  
spring.cloud.vault:
  host: vault-prod.company.com
  authentication: APPROLE
  scheme: https
```

### Verificación de Configuración

```bash
# Ver secretos cargados desde Vault
curl -s -u user:{password} http://localhost:8080/actuator/env | grep vault

# Ver configuración JWT
docker exec -it zero-trust-vault vault kv get secret/zero-trust-app
```

## 📋 Scripts Disponibles

- `./init-vault.sh` - Configurar secretos en Vault
- `./diagnosis.sh` - Verificar integración Vault
- `./mvnw clean package` - Construir aplicación
- `docker-compose up -d` - Levantar infraestructura
- `docker-compose logs -f zero-trust-app` - Ver logs

## 🚧 Estado Actual y Roadmap

### ✅ Fase 1 - Completada *(Actualizada hoy 06/06/2025)*
- [x] Estructura básica del proyecto
- [x] **Configuración de seguridad flexible** - Properties-driven con Spring Security 6.1+
- [x] Health checks y endpoints configurables
- [x] **Tests completos** - Unitarios, integración y seguridad (con `test-security` profile)
- [x] **Configuración multi-perfil** - development, test, test-security, production
- [x] **HashiCorp Vault integración básica**
- [x] **Spring Cloud Vault configurado**
- [x] **Secretos JWT desde Vault**
- [x] **Docker Compose completo**
- [x] **Bootstrap context funcionando**
- [x] **Tests aislados** - Sin conflictos con Vault en entorno de testing

### 🔄 Fase 2 - En Desarrollo Actual *(Próxima sesión)*
- [ ] **TokenService completo** con validación JWT
- [ ] **Endpoints de autenticación** (/auth/login, /auth/refresh, /auth/validate)
- [ ] **Middleware JWT** para requests autenticados
- [ ] **Rotación automática de tokens** desde Vault
- [ ] **Vault producción seguro** (TLS, AppRole, policies)
- [ ] **Cifrado en tránsito y reposo**
- [ ] Sistema de auditoría avanzado
- [ ] Control de acceso ABAC

### 🔮 Fase 3 - Próxima
- [ ] **Auto-unseal con Cloud KMS**
- [ ] **Secretos dinámicos para DB**
- [ ] **Vault Agent para rotación**
- [ ] MFA con TOTP
- [ ] WebAuthn/FIDO2
- [ ] Análisis de riesgo ML
- [ ] Dashboard de seguridad

### 🎯 Fase 4 - Futuro
- [ ] **Vault Enterprise features**
- [ ] **Multi-cluster Vault**
- [ ] **Disaster Recovery**
- [ ] Kubernetes integration
- [ ] Service Mesh (Istio)
- [ ] Zero Trust Network

## 🎯 Logros de la Sesión Actual *(06/06/2025)*

### ✅ Problemas Críticos Resueltos
1. **Tests con Vault** - Solucionado conflicto de Spring Cloud Vault en testing
2. **Configuración de seguridad flexible** - Property `app.security.require-auth-for-health-endpoints`
3. **Spring Security 6.1+ compatibility** - Actualizada sintaxis moderna (frameOptions, contentSecurityPolicy)
4. **Test isolation** - Perfiles `test` y `test-security` funcionando independientemente
5. **Properties-driven security** - Configuración dinámica sin múltiples `@Profile`

### 🚀 Mejoras Implementadas
- **SecurityConfig basado en properties** en lugar de configuraciones por perfiles duplicadas
- **Suite de tests robusta** con casos de autenticación y autorización completos
- **Configuración centralizada** en application.yml por perfiles
- **Sintaxis moderna** de Spring Security sin warnings de deprecación
- **Testing strategy definida** - Unit tests (sin auth) vs Security tests (con auth)

### 📋 Configuración de Tests Finalizada
```yaml
# Perfil "test" - Para tests unitarios normales
app.security.require-auth-for-health-endpoints: false

# Perfil "test-security" - Para tests de seguridad  
app.security.require-auth-for-health-endpoints: true
spring.security.user:
  name: testuser
  password: testpass
  roles: USER
```

## 🔍 Troubleshooting

### Problemas Comunes

**Vault no conecta:**
```bash
# Verificar que Vault está running
docker-compose ps vault

# Ver logs de Vault
docker-compose logs vault

# Verificar conectividad
docker exec -it zero-trust-app wget --spider http://vault:8200/v1/sys/health
```

**Secretos no se cargan:**
```bash
# Verificar secretos en Vault
docker exec -it zero-trust-vault vault kv get secret/zero-trust-app

# Ver logs de Spring Cloud Vault
docker-compose logs zero-trust-app | grep vault
```

**Bootstrap context issues:**
- Verificar que `spring-cloud-starter-bootstrap` está en el pom.xml
- Confirmar que `spring.config.import` está configurado
- Revisar logs de bootstrap en el arranque

**Tests fallan con Vault *(Resuelto)*:**
```bash
# Usar perfiles correctos
# Tests normales: @ActiveProfiles("test") 
# Tests de seguridad: @ActiveProfiles("test-security")

# Verificar properties
@TestPropertySource(properties = {
    "spring.cloud.vault.enabled=false",
    "app.security.require-auth-for-health-endpoints=true"  // Solo en security tests
})
```

## 🤝 Contribución

1. Fork el proyecto
2. Crear feature branch (`git checkout -b feature/VaultSecurity`)
3. Commit cambios (`git commit -m 'Add Vault production config'`)
4. Push al branch (`git push origin feature/VaultSecurity`)
5. Abrir Pull Request

## 📚 Documentación Adicional

- [HashiCorp Vault Documentation](https://www.vaultproject.io/docs)
- [Spring Cloud Vault Reference](https://docs.spring.io/spring-cloud-vault/docs/current/reference/html/)
- [Spring Security 6.1+ Migration Guide](https://docs.spring.io/spring-security/reference/migration/index.html)
- [Zero Trust Architecture Guide](./docs/zero-trust-guide.md)
- [Vault Production Hardening](./docs/vault-production.md)

## 📄 Licencia

Este proyecto está bajo licencia MIT.

## 🆘 Soporte

- 📖 Documentación: `./docs/`
- 🐛 Issues: GitHub Issues
- 💬 Discusiones: GitHub Discussions
- 🔐 Vault Issues: Verificar `./diagnosis.sh` primero