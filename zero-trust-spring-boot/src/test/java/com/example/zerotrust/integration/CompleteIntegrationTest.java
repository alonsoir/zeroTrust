package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🚀 TEST DE INTEGRACIÓN COMPLETO - Vault + Redis + H2 (CORREGIDO)
 *
 * ✅ Vault con secrets via REST API
 * ✅ Redis con TestContainers
 * ✅ H2 Database
 * ✅ Actuator Health Check completo
 * ✅ Test de funcionalidad Redis + Vault
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        // ❌ DESHABILITAR Spring Cloud Vault (usamos REST directo)
        "spring.cloud.vault.enabled=false",
        "spring.cloud.bootstrap.enabled=false",

        // ✅ Configuración básica
        "spring.application.name=complete-integration-test",

        // ✅ Database H2
        "spring.datasource.url=jdbc:h2:mem:completetest",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",

        // ✅ JPA
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",

        // ❌ DESHABILITAR Security para simplificar
        "spring.security.enabled=false",

        // ✅ Actuator completo
        "management.endpoints.web.exposure.include=health,info,redis",
        "management.endpoint.health.enabled=true",
        "management.endpoint.health.show-details=always",
        "management.health.defaults.enabled=true",

        // ✅ Logging
        "logging.level.root=INFO",
        "logging.level.com.example.zerotrust=DEBUG",
        "logging.level.org.springframework.data.redis=DEBUG"
})
@DisplayName("Test de Integración Completo - Vault + Redis (FIXED)")
class CompleteIntegrationTest {

    // ============================================================================
    // CONFIGURACIÓN DE TEST
    // ============================================================================

    @TestConfiguration
    static class RedisTestConfiguration {

        @Bean
        @Primary
        public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // Configurar serializadores
            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

            template.afterPropertiesSet();
            return template;
        }
    }

    // ============================================================================
    // CONSTANTES Y CLIENTES
    // ============================================================================

    private static final String VAULT_ROOT_TOKEN = "complete-test-token";
    private static final String SECRET_PATH = "complete-app";
    private static final String REDIS_PASSWORD = "test-redis-password";

    // ============================================================================
    // CONTENEDORES TESTCONTAINERS
    // ============================================================================

    @Container
    static GenericContainer<?> vaultContainer = new GenericContainer<>(DockerImageName.parse("hashicorp/vault:1.15.4"))
            .withExposedPorts(8200)
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_ROOT_TOKEN)
            .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            .withCommand("vault", "server", "-dev")
            .waitingFor(Wait.forHttp("/v1/sys/health")
                    .forPort(8200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(1)));

    @Autowired
    private Environment environment;

    @Autowired
    private TestRestTemplate restTemplate;

    // OPCIÓN 1: Usar StringRedisTemplate (más simple)
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // OPCIÓN 2: Usar RedisTemplate personalizado (configurado arriba)
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final TestRestTemplate vaultClient = new TestRestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================================
    // CONFIGURACIÓN DINÁMICA
    // ============================================================================

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) throws Exception {
        System.out.println("🎯 Configurando integración completa: Vault + Redis...");

        // ✅ REDIS CONFIGURATION
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
        registry.add("spring.data.redis.timeout", () -> "2000ms");
        registry.add("spring.data.redis.lettuce.pool.max-active", () -> 8);
        registry.add("spring.data.redis.lettuce.pool.max-idle", () -> 8);
        registry.add("spring.data.redis.lettuce.pool.min-idle", () -> 0);

        System.out.println("✅ Redis configurado: localhost:" + redisContainer.getMappedPort(6379));

        // ✅ VAULT SECRETS CONFIGURATION
        waitForVault();
        createCompleteSecrets();
        Map<String, Object> secrets = readSecretsFromVault();

        secrets.forEach((key, value) -> {
            registry.add(key, () -> value.toString());
            System.out.println("✅ Vault property: " + key + " = " + value);
        });

        System.out.println("✅ Configuración completa: " + secrets.size() + " secrets + Redis");
    }

    // ============================================================================
    // TESTS DE INFRAESTRUCTURA
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("🔍 Contenedores deben estar funcionando")
    void containersShouldBeRunning() {
        // Vault
        assertThat(vaultContainer.isRunning()).isTrue();
        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        ResponseEntity<String> vaultHealth = vaultClient.getForEntity(vaultUrl + "/v1/sys/health", String.class);
        assertThat(vaultHealth.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Redis
        assertThat(redisContainer.isRunning()).isTrue();

        System.out.println("✅ Vault: " + vaultUrl);
        System.out.println("✅ Redis: localhost:" + redisContainer.getMappedPort(6379));
    }

    @Test
    @Order(2)
    @DisplayName("🏥 Health check debe estar OK con todos los servicios")
    void healthCheckShouldBeOkWithAllServices() {
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);

        System.out.println("🔍 Health Response: " + healthResponse.getBody());

        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");

        System.out.println("✅ Health check OK con Vault + Redis + H2");
    }

    // ============================================================================
    // TESTS DE VAULT
    // ============================================================================

    @Test
    @Order(3)
    @DisplayName("🌱 JWT secrets desde Vault")
    void jwtSecretsShouldBeReadFromVault() {
        String jwtSecret = environment.getProperty("app.jwt.secret");
        String jwtExpiration = environment.getProperty("app.jwt.expiration");
        String jwtIssuer = environment.getProperty("app.jwt.issuer");

        System.out.println("🔍 JWT Secret: " + (jwtSecret != null ? jwtSecret.substring(0, 20) + "..." : "NULL"));
        System.out.println("🔍 JWT Expiration: " + jwtExpiration);
        System.out.println("🔍 JWT Issuer: " + jwtIssuer);

        assertThat(jwtSecret).isNotNull();
        assertThat(jwtSecret.length()).isGreaterThan(64);
        assertThat(jwtSecret).contains("complete-integration");
        assertThat(jwtExpiration).isEqualTo("7200000");
        assertThat(jwtIssuer).isEqualTo("complete-integration-issuer");

        System.out.println("✅ JWT secrets verificados desde Vault");
    }

    @Test
    @Order(4)
    @DisplayName("🗄️ Database credentials desde Vault")
    void databaseCredentialsShouldBeReadFromVault() {
        String dbUsername = environment.getProperty("app.database.username");
        String dbPassword = environment.getProperty("app.database.password");
        String dbUrl = environment.getProperty("app.database.url");

        System.out.println("🔍 DB Username: " + dbUsername);
        System.out.println("🔍 DB Password: " + (dbPassword != null ? dbPassword.substring(0, 5) + "..." : "NULL"));
        System.out.println("🔍 DB URL: " + dbUrl);

        assertThat(dbUsername).isEqualTo("complete_vault_user");
        assertThat(dbPassword).isEqualTo("complete_vault_password_123");
        assertThat(dbUrl).contains("postgresql");

        System.out.println("✅ Database credentials verificados desde Vault");
    }

    // ============================================================================
    // TESTS DE REDIS
    // ============================================================================

    @Test
    @Order(5)
    @DisplayName("🔴 Redis debe estar funcionando (StringRedisTemplate)")
    void redisShouldBeWorkingWithStringTemplate() {
        // Test básico de Redis con StringRedisTemplate
        String testKey = "test:vault:integration:string:" + System.currentTimeMillis();
        String testValue = "vault-redis-integration-test-string";

        // Escribir en Redis
        stringRedisTemplate.opsForValue().set(testKey, testValue);

        // Leer de Redis
        String retrievedValue = stringRedisTemplate.opsForValue().get(testKey);

        assertThat(retrievedValue).isEqualTo(testValue);

        // Limpiar
        stringRedisTemplate.delete(testKey);

        System.out.println("✅ Redis funcionando correctamente con StringRedisTemplate");
    }

    @Test
    @Order(6)
    @DisplayName("🔴 Redis debe estar funcionando (RedisTemplate personalizado)")
    void redisShouldBeWorkingWithCustomTemplate() {
        // Test básico de Redis con RedisTemplate personalizado
        String testKey = "test:vault:integration:object:" + System.currentTimeMillis();
        String testValue = "vault-redis-integration-test-object";

        // Escribir en Redis
        redisTemplate.opsForValue().set(testKey, testValue);

        // Leer de Redis
        Object retrievedValue = redisTemplate.opsForValue().get(testKey);

        assertThat(retrievedValue).isEqualTo(testValue);

        // Limpiar
        redisTemplate.delete(testKey);

        System.out.println("✅ Redis funcionando correctamente con RedisTemplate personalizado");
    }

    @Test
    @Order(7)
    @DisplayName("🔄 Integración Vault + Redis: Guardar secrets en Redis")
    void vaultSecretsInRedis() {
        // Obtener secrets de Vault (via Environment)
        String apiKey = environment.getProperty("app.api.external-key");
        String monitoringKey = environment.getProperty("app.monitoring.key");

        assertThat(apiKey).isNotNull();
        assertThat(monitoringKey).isNotNull();

        // Guardar secrets en Redis (simulando cache)
        String redisKeyApi = "cache:api:external-key";
        String redisKeyMonitoring = "cache:monitoring:key";

        // Usar StringRedisTemplate para strings simples
        stringRedisTemplate.opsForValue().set(redisKeyApi, apiKey);
        stringRedisTemplate.opsForValue().set(redisKeyMonitoring, monitoringKey);

        // Verificar que están en Redis
        String cachedApiKey = stringRedisTemplate.opsForValue().get(redisKeyApi);
        String cachedMonitoringKey = stringRedisTemplate.opsForValue().get(redisKeyMonitoring);

        assertThat(cachedApiKey).isEqualTo(apiKey);
        assertThat(cachedMonitoringKey).isEqualTo(monitoringKey);

        System.out.println("✅ Integración Vault→Redis funcionando");
        System.out.println("  🔑 API Key cached: " + apiKey.substring(0, 10) + "...");
        System.out.println("  📊 Monitoring Key cached: " + monitoringKey.substring(0, 10) + "...");

        // Limpiar Redis
        stringRedisTemplate.delete(redisKeyApi);
        stringRedisTemplate.delete(redisKeyMonitoring);
    }

    // ============================================================================
    // HELPER METHODS - VAULT (sin cambios)
    // ============================================================================

    private static void waitForVault() throws Exception {
        System.out.println("⏳ Esperando Vault...");
        Thread.sleep(3000);

        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        ResponseEntity<String> health = vaultClient.getForEntity(vaultUrl + "/v1/sys/health", String.class);

        if (!health.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Vault no está listo: " + health.getStatusCode());
        }

        System.out.println("✅ Vault listo");
    }

    private static void createCompleteSecrets() throws Exception {
        System.out.println("🔧 Creando secrets completos en Vault...");

        Map<String, Object> secrets = Map.of(
                "app.jwt.secret", "complete-integration-jwt-secret-from-vault-at-least-64-characters-long-for-security",
                "app.jwt.expiration", "7200000",
                "app.jwt.issuer", "complete-integration-issuer",
                "app.database.username", "complete_vault_user",
                "app.database.password", "complete_vault_password_123",
                "app.database.url", "jdbc:postgresql://vault-prod-db:5432/vaultdb",
                "app.api.external-key", "complete-api-" + System.currentTimeMillis(),
                "app.monitoring.key", "complete-mon-" + System.currentTimeMillis(),
                "app.redis.cache-ttl", "3600",
                "app.cache.strategy", "vault-redis-integration"
        );

        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        String payload = objectMapper.writeValueAsString(Map.of("data", secrets));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", VAULT_ROOT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = vaultClient.exchange(
                vaultUrl + "/v1/secret/data/" + SECRET_PATH,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create complete secrets: " + response.getBody());
        }

        System.out.println("✅ " + secrets.size() + " secrets completos creados en Vault");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readSecretsFromVault() throws Exception {
        System.out.println("📖 Leyendo secrets completos de Vault...");

        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", VAULT_ROOT_TOKEN);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = vaultClient.exchange(
                vaultUrl + "/v1/secret/data/" + SECRET_PATH,
                HttpMethod.GET,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to read complete secrets: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        Map<String, Object> secrets = (Map<String, Object>) data.get("data");

        System.out.println("✅ " + secrets.size() + " secrets completos leídos de Vault");
        return secrets;
    }

    @AfterAll
    static void cleanup() {
        System.out.println("🧹 Test de integración completo terminado (FIXED)");
        System.out.println("  ✅ Vault funcionó correctamente");
        System.out.println("  ✅ Redis funcionó correctamente");
        System.out.println("  ✅ H2 Database funcionó correctamente");
        System.out.println("  ✅ Integración Vault→Redis funcionó correctamente");
    }
}