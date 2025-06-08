package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🎯 TEST MÍNIMO - Solo Vault + H2 (Sin Redis)
 *
 * ✅ Para aplicaciones que NO usan Redis
 * ✅ Vault con secrets via REST API
 * ✅ H2 Database
 * ✅ Health check básico sin Redis
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // ❌ EXCLUIR Redis AUTO-CONFIGURATIONS completamente
                "spring.autoconfigure.exclude[0]=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.autoconfigure.exclude[1]=org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
                "spring.autoconfigure.exclude[2]=org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",

                // ❌ DESHABILITAR Vault Cloud
                "spring.cloud.vault.enabled=false",
                "spring.cloud.bootstrap.enabled=false",

                // ✅ App básica
                "spring.application.name=minimal-vault-test",

                // ✅ H2 Database
                "spring.datasource.url=jdbc:h2:mem:minimaltest",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",

                // ❌ DESHABILITAR Security
                "spring.security.enabled=false",

                // ✅ Health check básico (sin Redis)
                "management.endpoints.web.exposure.include=health,info",
                "management.endpoint.health.enabled=true",
                "management.health.defaults.enabled=true",

                // ✅ Logging
                "logging.level.root=INFO",
                "logging.level.com.example.zerotrust=DEBUG"
        }
)
@Testcontainers
@DisplayName("Test Mínimo - Solo Vault (Sin Redis)")
class MinimalVaultTest {

    private static final String VAULT_ROOT_TOKEN = "minimal-test-token";
    private static final String SECRET_PATH = "minimal-app";

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
     * 🎯 CONFIGURACIÓN MÍNIMA - Solo Vault secrets
     */
    @DynamicPropertySource
    static void configureMinimalTest(DynamicPropertyRegistry registry) throws Exception {
        System.out.println("🎯 Configuración mínima - Solo Vault...");

        waitForVault();
        createMinimalSecrets();
        Map<String, Object> secrets = readSecretsFromVault();

        secrets.forEach((key, value) -> {
            registry.add(key, () -> value.toString());
            System.out.println("✅ " + key + " = " + value);
        });

        System.out.println("✅ Configuración mínima completa: " + secrets.size() + " secrets");
    }

    @Test
    @Order(1)
    @DisplayName("🔍 Vault funcionando")
    void vaultShouldBeRunning() {
        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        ResponseEntity<String> response = vaultClient.getForEntity(vaultUrl + "/v1/sys/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Vault OK: " + vaultUrl);
    }

    @Test
    @Order(2)
    @DisplayName("🏥 Health check OK (sin Redis)")
    void healthCheckShouldBeOkWithoutRedis() {
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);

        System.out.println("🔍 Health: " + healthResponse.getStatusCode());
        System.out.println("🔍 Body: " + healthResponse.getBody());

        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");

        // Verificar que NO hay health checks de Redis
        assertThat(healthResponse.getBody()).doesNotContain("redis");

        System.out.println("✅ Health check OK sin Redis");
    }

    @Test
    @Order(3)
    @DisplayName("🌱 JWT secrets desde Vault")
    void jwtSecretsShouldWork() {
        String jwtSecret = environment.getProperty("app.jwt.secret");
        String jwtExpiration = environment.getProperty("app.jwt.expiration");

        System.out.println("🔍 JWT Secret: " + (jwtSecret != null ? jwtSecret.substring(0, 20) + "..." : "NULL"));
        System.out.println("🔍 Length: " + (jwtSecret != null ? jwtSecret.length() : 0));

        assertThat(jwtSecret).isNotNull();
        assertThat(jwtSecret.length()).isGreaterThan(64);
        assertThat(jwtExpiration).isEqualTo("7200000");

        System.out.println("✅ JWT secrets OK desde Vault");
    }

    @Test
    @Order(4)
    @DisplayName("🗄️ Database credentials desde Vault")
    void databaseCredentialsShouldWork() {
        String dbUsername = environment.getProperty("app.database.username");
        String dbPassword = environment.getProperty("app.database.password");

        assertThat(dbUsername).isEqualTo("minimal_vault_user");
        assertThat(dbPassword).isEqualTo("minimal_vault_password");

        System.out.println("✅ DB credentials OK: " + dbUsername);
    }

    // ============================================================================
    // VAULT HELPERS
    // ============================================================================

    private static void waitForVault() throws Exception {
        Thread.sleep(2000);
        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        ResponseEntity<String> health = vaultClient.getForEntity(vaultUrl + "/v1/sys/health", String.class);

        if (!health.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Vault not ready");
        }
    }

    private static void createMinimalSecrets() throws Exception {
        Map<String, Object> secrets = Map.of(
                "app.jwt.secret", "minimal-jwt-secret-from-vault-at-least-64-characters-long-for-security",
                "app.jwt.expiration", "7200000",
                "app.database.username", "minimal_vault_user",
                "app.database.password", "minimal_vault_password"
        );

        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        String payload = objectMapper.writeValueAsString(Map.of("data", secrets));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", VAULT_ROOT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = vaultClient.exchange(
                vaultUrl + "/v1/secret/data/" + SECRET_PATH,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create secrets");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readSecretsFromVault() throws Exception {
        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", VAULT_ROOT_TOKEN);

        ResponseEntity<String> response = vaultClient.exchange(
                vaultUrl + "/v1/secret/data/" + SECRET_PATH,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        return (Map<String, Object>) data.get("data");
    }

    @AfterAll
    static void cleanup() {
        System.out.println("🧹 Test mínimo completado - Solo Vault funcionó OK");
    }
}