package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🎯 VAULT TEST SIMPLE - Sin Spring Cloud Vault, solo REST API
 *
 * ✅ NO usa Spring Cloud Vault (evita problemas de Bootstrap)
 * ✅ Lee secrets directamente de Vault via REST
 * ✅ Inyecta properties manualmente via @DynamicPropertySource
 * ✅ Completamente independiente y controlado
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        // ❌ DESHABILITAR Spring Cloud Vault completamente
        "spring.cloud.vault.enabled=false",
        "spring.cloud.bootstrap.enabled=false",

        // ✅ Configuración básica de la aplicación
        "spring.application.name=simple-vault-test",

        // ✅ Database H2
        "spring.datasource.url=jdbc:h2:mem:simpletest",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",

        // ✅ JPA
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",

        // ❌ DESHABILITAR Redis COMPLETAMENTE
        "spring.data.redis.enabled=false",
        "spring.autoconfigure.exclude[0]=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "spring.autoconfigure.exclude[1]=org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
        "spring.autoconfigure.exclude[2]=org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",

        // ❌ DESHABILITAR Health Indicators de Redis
        "management.health.redis.enabled=false",
        "management.health.redisReactive.enabled=false",

        // ❌ DESHABILITAR Security
        "spring.security.enabled=false",

        // ✅ Actuator - Solo health básico
        "management.endpoints.web.exposure.include=health,info",
        "management.endpoint.health.enabled=true",
        "management.health.defaults.enabled=true",

        // ✅ Logging
        "logging.level.root=INFO",
        "logging.level.com.example.zerotrust=DEBUG",
        "logging.level.org.springframework.data.redis=ERROR"
})
@DisplayName("Vault Test Simplificado - REST API")
class SimpleVaultTest {

    private static final String VAULT_ROOT_TOKEN = "simple-test-token";
    private static final String SECRET_PATH = "simple-app";

    @Container
    static GenericContainer<?> vaultContainer = new GenericContainer<>(DockerImageName.parse("hashicorp/vault:1.15.4"))
            .withExposedPorts(8200)
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_ROOT_TOKEN)
            .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            .withCommand("vault", "server", "-dev")
            .waitingFor(Wait.forHttp("/v1/sys/health")
                    .forPort(8200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @Autowired
    private Environment environment;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final TestRestTemplate vaultClient = new TestRestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 🎯 CONFIGURACIÓN DINÁMICA - Lee de Vault y configura properties
     */
    @DynamicPropertySource
    static void configureWithVaultSecrets(DynamicPropertyRegistry registry) throws Exception {
        System.out.println("🎯 Configurando properties desde Vault via REST...");

        // 1. Esperar a que Vault esté completamente listo
        waitForVault();

        // 2. Crear secrets en Vault
        createTestSecrets();

        // 3. Leer secrets de Vault
        Map<String, Object> secrets = readSecretsFromVault();

        // 4. Configurar properties dinámicamente
        secrets.forEach((key, value) -> {
            registry.add(key, () -> value.toString());
            System.out.println("✅ Property configurada: " + key + " = " + value);
        });

        System.out.println("✅ " + secrets.size() + " properties configuradas desde Vault");
    }

    @Test
    @Order(1)
    @DisplayName("🔍 Vault debe estar funcionando")
    void vaultShouldBeRunning() {
        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        ResponseEntity<String> response = vaultClient.getForEntity(vaultUrl + "/v1/sys/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Vault funcionando en: " + vaultUrl);
    }

    @Test
    @Order(2)
    @DisplayName("🌱 JWT secret debe leerse desde Vault")
    void jwtSecretShouldBeReadFromVault() {
        String jwtSecret = environment.getProperty("app.jwt.secret");
        String jwtExpiration = environment.getProperty("app.jwt.expiration");
        String jwtIssuer = environment.getProperty("app.jwt.issuer");

        System.out.println("🔍 JWT Secret: " + (jwtSecret != null ? jwtSecret.substring(0, 20) + "..." : "NULL"));
        System.out.println("🔍 Longitud: " + (jwtSecret != null ? jwtSecret.length() : 0));
        System.out.println("🔍 Expiration: " + jwtExpiration);
        System.out.println("🔍 Issuer: " + jwtIssuer);

        // Verificaciones
        assertThat(jwtSecret).isNotNull();
        assertThat(jwtSecret.length()).isGreaterThan(64);
        assertThat(jwtSecret).contains("vault-rest-api");
        assertThat(jwtExpiration).isEqualTo("7200000");
        assertThat(jwtIssuer).isEqualTo("simple-vault-issuer");

        System.out.println("✅ JWT Properties verificadas desde Vault");
    }

    @Test
    @Order(3)
    @DisplayName("🗄️ Database credentials desde Vault")
    void databaseCredentialsShouldBeReadFromVault() {
        String dbUsername = environment.getProperty("app.database.username");
        String dbPassword = environment.getProperty("app.database.password");

        System.out.println("🔍 DB Username: " + dbUsername);
        System.out.println("🔍 DB Password: " + (dbPassword != null ? dbPassword.substring(0, 5) + "..." : "NULL"));

        assertThat(dbUsername).isEqualTo("vault_rest_user");
        assertThat(dbPassword).isEqualTo("vault_rest_password_123");

        System.out.println("✅ Database credentials verificadas desde Vault");
    }

    @Test
    @Order(4)
    @DisplayName("🔑 API keys desde Vault")
    void apiKeysShouldBeReadFromVault() {
        String apiKey = environment.getProperty("app.api.external-key");
        String monitoringKey = environment.getProperty("app.monitoring.key");

        System.out.println("🔍 API Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "NULL"));
        System.out.println("🔍 Monitoring Key: " + (monitoringKey != null ? monitoringKey.substring(0, 10) + "..." : "NULL"));

        assertThat(apiKey).isNotNull();
        assertThat(apiKey).startsWith("api-rest-");
        assertThat(monitoringKey).isNotNull();
        assertThat(monitoringKey).startsWith("mon-rest-");

        System.out.println("✅ API Keys verificadas desde Vault");
    }

    @Test
    @Order(5)
    @DisplayName("🏥 Aplicación funcionando con secrets de Vault")
    void applicationShouldWorkWithVaultSecrets() {
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Aplicación funcionando correctamente con secrets de Vault");
    }

    // ============================================================================
    // HELPER METHODS - Interacción directa con Vault
    // ============================================================================

    private static void waitForVault() throws Exception {
        System.out.println("⏳ Esperando a que Vault esté completamente listo...");
        Thread.sleep(3000); // Dar tiempo extra

        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        ResponseEntity<String> health = vaultClient.getForEntity(vaultUrl + "/v1/sys/health", String.class);

        if (!health.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Vault no está listo: " + health.getStatusCode());
        }

        System.out.println("✅ Vault está listo");
    }

    private static void createTestSecrets() throws Exception {
        System.out.println("🔧 Creando secrets en Vault...");

        Map<String, Object> secrets = Map.of(
                "app.jwt.secret", "simple-jwt-secret-from-vault-rest-api-at-least-64-characters-long-for-security",
                "app.jwt.expiration", "7200000",
                "app.jwt.issuer", "simple-vault-issuer",
                "app.database.username", "vault_rest_user",
                "app.database.password", "vault_rest_password_123",
                "app.api.external-key", "api-rest-" + System.currentTimeMillis(),
                "app.monitoring.key", "mon-rest-" + System.currentTimeMillis()
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
            throw new RuntimeException("Failed to create secrets: " + response.getBody());
        }

        System.out.println("✅ " + secrets.size() + " secrets creados en Vault");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readSecretsFromVault() throws Exception {
        System.out.println("📖 Leyendo secrets de Vault...");

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
            throw new RuntimeException("Failed to read secrets: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        Map<String, Object> secrets = (Map<String, Object>) data.get("data");

        System.out.println("✅ " + secrets.size() + " secrets leídos de Vault");
        return secrets;
    }

    @AfterAll
    static void cleanup() {
        System.out.println("🧹 Simple Vault Test completado");
    }
}