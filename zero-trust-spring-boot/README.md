# Zero Trust Spring Boot Application

Una implementación empresarial de arquitectura Zero Trust con Spring Boot 3.3.5 y gestión centralizada de secretos con HashiCorp Vault.

## 🎯 Características

- ✅ **Gestión de secretos con HashiCorp Vault** - Secretos centralizados y seguros
- ✅ **Spring Cloud Vault** integrado - Lectura automática de secretos
- ✅ **Autenticación JWT** con secretos rotativos desde Vault
- ✅ **ZERO secrets hardcodeados** - Eliminación completa de vulnerabilidades 🔥 *NUEVO*
- ✅ **Secrets criptográficamente seguros** - Generación con SecureRandom (80+ chars) 🔥 *NUEVO*
- ✅ **Metadatos de auditoría** - Versioning, timestamps y source tracking 🔥 *NUEVO*
- ✅ **Fail-fast validation** - Aplicación no arranca sin secrets válidos 🔥 *NUEVO*
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
- **🛡️ Zero Trust compliant** - NO secrets hardcodeados *NUEVO*
- **🔐 Generación segura** - SecureRandom con validación mínima de 64 caracteres *NUEVO*

### Secretos Gestionados
- `app.jwt.secret` - Clave secreta para JWT tokens (criptográficamente segura, rotativo) *ACTUALIZADO*
- `app.jwt.secretVersion` - Versioning para auditoría y rotación *NUEVO*
- `app.jwt.secretCreatedAt` - Timestamp de creación para control TTL *NUEVO*
- `app.database.*` - Credenciales de base de datos
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
| `/actuator/health` | Health check de Actuator + Vault | ✅ |
| `/actuator/env` | Variables de entorno (requiere auth) | 🔒 |
| `/actuator/configprops` | Propiedades de configuración | 🔒 |
| `/actuator/vault` | Estado de integración con Vault | 🔒 *NUEVO* |
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
5. **🛡️ ZERO secrets hardcodeados** - Eliminación completa de vulnerabilidades *NUEVO*
6. **🔐 Fail-fast security** - Aplicación no arranca sin secrets válidos *NUEVO*

### Gestión de Secretos *(ACTUALIZADO)*
- **Vault Integration**: Spring Cloud Vault para lectura automática
- **Bootstrap Context**: Carga de secretos antes del contexto principal
- **Property Sources**: Vault tiene prioridad sobre configuraciones locales
- **🚨 NO Fallback Values**: Secrets OBLIGATORIOS desde Vault (production) *NUEVO*
- **🔐 Secure Generation**: Generación criptográficamente segura con SecureRandom *NUEVO*
- **📊 Audit Metadata**: Versioning + timestamps + source tracking *NUEVO*
- **⚡ Fail-Fast Validation**: @PostConstruct validation en JwtProperties *NUEVO*

### Headers de Seguridad
- Content Security Policy (CSP)
- X-Frame-Options: SAMEORIGIN (para H2 Console)
- Session Management: STATELESS

## 🧪 Testing *(ACTUALIZADO)*

```bash
# Tests unitarios
./mvnw test

# Test específico de integración Vault SEGURA
./mvnw test -Dtest=Step2_SpringBootVaultAutomaticTest

# Tests de integración
./mvnw verify

# Suite completa con Docker
./scripts/test.sh

# Verificar integración con Vault
./diagnosis.sh
```

### 🔥 Tests de Seguridad Zero Trust *(NUEVO)*
```bash
# Validar que NO hay secrets hardcodeados
./mvnw test -Dtest=Step2_SpringBootVaultAutomaticTest#compareHardcodedVsZeroTrust

# Verificar generación segura de secrets
./mvnw test -Dtest=Step2_SpringBootVaultAutomaticTest#configurationPropertiesShouldBeSecureFromVault

# Comprobar fail-fast validation
./mvnw test -Dtest=Step2_SpringBootVaultAutomaticTest#shouldFailWithoutSecret
```

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

## 🔧 Configuración Avanzada

### Perfiles de Spring

- **development**: H2 + Vault dev mode, logs debug, **fallback secrets SOLO si Vault no disponible**
- **test**: H2 en memoria, Vault deshabilitado, **secrets fijos para testing**
- **production**: PostgreSQL + Vault production, TLS habilitado, **ZERO fallbacks - fail-fast**

### Configuración de Vault por Perfil *(ACTUALIZADA)*

```yaml
    # Development - Con fallback de emergencia
    spring.cloud.vault:
      host: localhost
      token: dev-root-token
      scheme: http
      fail-fast: false  # Permisivo en desarrollo

    app.jwt:
      secret: ${VAULT_JWT_SECRET:fallback-only-for-dev-64-chars-minimum}

    # Production - OBLIGATORIO desde Vault
    spring.cloud.vault:
      host: vault-prod.company.com
      authentication: APPROLE
      scheme: https
      fail-fast: true   # STRICT - no arranca sin Vault
    
    app.jwt:
      # ❌ NO SECRET AQUÍ - viene 100% de Vault o la app NO ARRANCA
```

### 🔐 Validación de Secretos Seguros *(NUEVO)*

```bash
# Verificar que el secret es seguro
curl -s -u user:{password} http://localhost:8080/actuator/configprops | jq '.app.jwt'

# Verificar metadatos de auditoría
curl -s -u user:{password} http://localhost:8080/actuator/env | grep -E "(secretVersion|secretCreatedAt|secretFromVault)"

# Ver longitud del secret (debe ser 100+ caracteres)
docker exec -it zero-trust-vault vault kv get -field=secret secret/zero-trust-app | wc -c
```

## 📋 Scripts Disponibles

- `./init-vault.sh` - Configurar secretos en Vault
- `./diagnosis.sh` - Verificar integración Vault
- `./mvnw clean package` - Construir aplicación
- `docker-compose up -d` - Levantar infraestructura
- `docker-compose logs -f zero-trust-app` - Ver logs

## 🚧 Estado Actual y Roadmap

### ✅ Fase 1 - Completada *(Actualizada 07/06/2025)*
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

### 🔥 **Fase 2 - COMPLETADA HOY *(07/06/2025)* - SEGURIDAD ZERO TRUST**
- [x] **🚨 VULNERABILIDAD CRÍTICA ELIMINADA** - NO más secrets hardcodeados
- [x] **🔐 Secrets criptográficamente seguros** - Generación con SecureRandom (80+ chars)
- [x] **📊 Sistema de auditoría de secrets** - Versioning + timestamps + source tracking
- [x] **⚡ Fail-fast validation** - @PostConstruct validation en JwtProperties
- [x] **🧪 Test integración Vault segura** - Step2_SpringBootVaultAutomaticTest completo
- [x] **🔧 Configuración alineada test/producción** - Misma estructura JwtProperties
- [x] **🛡️ Configuración Zero Trust** - Vault obligatorio en production, fallbacks solo en dev
- [x] **📋 Metadatos para rotación** - secretVersion, secretCreatedAt, secretFromVault
- [x] **🎯 Preparación PASO 3** - Infraestructura completa para rotación automática

### 🔄 Fase 3 - PRÓXIMA SESIÓN *(Rotación Automática)*
- [ ] **🔄 Rotación automática de secrets JWT** - Scheduled rotation con TTL
- [ ] **⏲️ TTL configurable en Vault** - Políticas de expiración automática
- [ ] **📈 Métricas de rotación** - Success/failure rates, timing metrics
- [ ] **🔔 Alertas de expiración** - Notifications antes de expiración
- [ ] **🌊 Hot-reload de secrets** - Actualización sin restart de aplicación
- [ ] **🗄️ Dynamic Database Secrets** - Credenciales temporales desde Vault
- [ ] **🔍 Health checks avanzados** - Monitoreo de estado de secrets
- [ ] **📊 Dashboard de rotación** - UI para monitoring de secrets lifecycle

### 🎯 Fase 4 - AVANZADA *(Próximas semanas)*
- [ ] **TokenService completo** con validación JWT
- [ ] **Endpoints de autenticación** (/auth/login, /auth/refresh, /auth/validate)
- [ ] **Middleware JWT** para requests autenticados
- [ ] **Vault producción seguro** (TLS, AppRole, policies)
- [ ] **Auto-unseal con Cloud KMS**
- [ ] **Vault Agent para rotación**
- [ ] **Cifrado en tránsito y reposo**
- [ ] Control de acceso ABAC

### 🔮 Fase 5 - ENTERPRISE
- [ ] **Vault Enterprise features**
- [ ] **Multi-cluster Vault**
- [ ] **Disaster Recovery**
- [ ] MFA con TOTP
- [ ] WebAuthn/FIDO2
- [ ] Análisis de riesgo ML
- [ ] Kubernetes integration
- [ ] Service Mesh (Istio)
- [ ] Zero Trust Network

## 🎯 Logros de la Sesión Actual *(07/06/2025)*

### 🚨 **VULNERABILIDAD CRÍTICA RESUELTA**
**ANTES (INSEGURO):**
```java
// ❌ Secret visible en código fuente
private String secret = "zero-trust-default-secret-key-change-in-production...";
```

**AHORA (SEGURO):**
```java
// ✅ Secret OBLIGATORIO desde Vault, 107 caracteres seguros
private String secret; // Viene 100% de Vault con fail-fast validation
```

### ✅ Transformaciones Implementadas
1. **🔐 Generación segura**: SecureRandom + 107 caracteres criptográficamente seguros
2. **📊 Auditoría completa**: Versioning, timestamps, source tracking
3. **⚡ Fail-fast validation**: Aplicación NO arranca sin secret válido desde Vault
4. **🧪 Tests alineados**: Misma estructura test/producción para copy/paste directo
5. **🛡️ Configuración Zero Trust**: Vault obligatorio en production, fallbacks solo en dev
6. **📋 Metadatos preparados**: Infrastructure completa para rotación automática

### 🔧 **Archivos Refactorizados Hoy**
- `Step2_SpringBootVaultAutomaticTest.java` - Test completo con clases alineadas y generación segura
- `JwtProperties.java` - Clase de producción con validación fail-fast y metadatos
- `application.yml` - Configuración segura por perfiles sin secrets hardcodeados
- `application-step-2.yml` - Configuración de test sin duplicación ni vulnerabilidades

### 📊 **Métricas de Seguridad Logradas**
```
✅ Secret length: 107 caracteres (mínimo requerido: 64)
✅ Secret source: Vault (NO hardcoded)
✅ Fail-fast validation: ACTIVA
✅ Audit metadata: Completo (version + timestamp + source)
✅ Zero hardcoded fallbacks: En producción
✅ Rotation preparedness: 100%
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

**🚨 SECURITY VIOLATION: JWT secret is REQUIRED *(NUEVO)*:**
```bash
# Este error es CORRECTO - indica que la validación fail-fast funciona
# Verificar que Vault tiene el secret:
docker exec -it zero-trust-vault vault kv get -field=app.jwt.secret secret/zero-trust-app

# Si no existe, ejecutar:
./init-vault.sh
```

**Bootstrap context issues:**
- Verificar que `spring-cloud-starter-bootstrap` está en el pom.xml
- Confirmar que `spring.config.import` está configurado
- Revisar logs de bootstrap en el arranque

**Tests fallan con Vault *(Resuelto)*:**
```bash
# Usar el test correcto que genera secrets seguros
./mvnw test -Dtest=Step2_SpringBootVaultAutomaticTest

# Verificar logs de generación segura:
# 🔐 JWT Secret: 107 caracteres (✅ >64)
# ✅ Secret desde Vault: true
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
- **[Secrets Security Best Practices](./docs/secrets-security.md)** *NUEVO*

## 📄 Licencia

Este proyecto está bajo licencia MIT.

## 🆘 Soporte

- 📖 Documentación: `./docs/`
- 🐛 Issues: GitHub Issues
- 💬 Discusiones: GitHub Discussions
- 🔐 Vault Issues: Verificar `./diagnosis.sh` primero
- **🚨 Security Issues: Verificar fail-fast validation con `./mvnw test -Dtest=Step2_SpringBootVaultAutomaticTest`** *NUEVO*

---

## 🎉 Reconocimientos

**Sesión del 07/06/2025**: Transformación crítica de seguridad completada - de aplicación vulnerable con secrets hardcodeados a Zero Trust compliant con secrets criptográficamente seguros desde Vault.

**¡5 horas intensas eliminando vulnerabilidades y preparando rotación automática!** 🛡️🚀