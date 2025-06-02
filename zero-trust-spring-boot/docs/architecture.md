# Arquitectura Zero Trust

## Principios Fundamentales

### 1. Verificación Continua
- Cada request es verificado independientemente
- No hay sesiones persistentes confiables
- Tokens de corta duración (15 minutos por defecto)

### 2. Control de Acceso Basado en Atributos (ABAC)
- Usuario + Contexto + Recurso + Políticas
- Evaluación dinámica en tiempo real
- No solo roles estáticos

### 3. Auditoría Radical
- Cada operación genera eventos de auditoría
- Trazabilidad completa
- Análisis de comportamiento en tiempo real

## Componentes de la Aplicación

### Backend (Spring Boot)
- **TokenService**: Gestión de JWT seguros con revocación
- **ContextService**: Verificación de contexto y riesgo
- **AuditService**: Logging y trazabilidad completa
- **PolicyEngine**: Evaluación ABAC dinámica

### Seguridad por Capas
1. **Network**: Contenedores aislados
2. **Transport**: TLS 1.3 obligatorio
3. **Application**: JWT + verificación continua
4. **Data**: Encriptación en reposo

### Infraestructura
- **H2**: Base de datos en memoria para desarrollo
- **PostgreSQL**: Datos persistentes y auditoría (producción)
- **Redis**: Cache de tokens y gestión de sesiones
- **Kafka**: Cola de eventos de auditoría asíncrona
- **Undertow**: Servidor web optimizado

## Modelo de Datos

### Usuarios y Roles
- Sistema flexible de roles y permisos
- Soporte para MFA obligatorio
- Tracking de dispositivos

### Auditoría
- Eventos inmutables con hash de integridad
- Metadatos contextuales completos
- Análisis de patrones sospechosos

## Configuración de Seguridad

### Headers de Seguridad
- Content Security Policy (CSP)
- HTTP Strict Transport Security (HSTS)
- X-Frame-Options: SAMEORIGIN (para H2 Console)
- X-Content-Type-Options: nosniff

### Contenedores
- Usuario no-root (nonroot)
- Filesystem de solo lectura
- Capabilities mínimas
- Red aislada

## Flujo de Autenticación

1. **Login Request** → Validación credenciales
2. **Context Analysis** → Evaluación de riesgo
3. **Token Generation** → JWT con claims específicos
4. **Continuous Verification** → Cada request validado
5. **Risk Assessment** → Análisis continuo de comportamiento

## Endpoints de Seguridad

### Públicos
- `/api/health` - Health check
- `/api/info` - Información de la aplicación
- `/actuator/health` - Actuator health
- `/h2-console/**` - Base de datos (solo desarrollo)

### Protegidos
- Cualquier otro endpoint requiere autenticación JWT válida
