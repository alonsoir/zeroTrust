package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * 🎯 TEST 3: API Keys desde Vault - COMPLETAMENTE INDEPENDIENTE
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Test 3: API Keys desde Vault")
class ApiKeysVaultTest {

    private static final String VAULT_ROOT_TOKEN = "api-test-token";
    private static final String APP_SECRET_PATH = "api-app";

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
     * 🎯 CONFIGURACIÓN COMPLETA PARA API KEYS TEST
     */
    @DynamicPropertySource
    static void configureApiKeysTest(DynamicPropertyRegistry registry) {
        System.out.println("🎯 Configurando API Keys Test - Completamente independiente");

        // Vault configuration
        registry.add("spring.cloud.bootstrap.enabled", () -> false);
        registry.add("spring.cloud.vault.enabled", () -> true);
        registry.add("spring.cloud.vault.config.enabled", () -> true);
        registry.add("spring.cloud.vault.host", () -> "localhost");
        registry.add("spring.cloud.vault.port", () -> vaultContainer.getMappedPort(8200));
        registry.add("spring.cloud.vault.scheme", () -> "http");
        registry.add("spring.cloud.vault.token", () -> VAULT_ROOT_TOKEN);
        registry.add("spring.cloud.vault.authentication", () -> "token");
        registry.add("spring.cloud.vault.kv.enabled", () -> true);
        registry.add("spring.cloud.vault.kv.backend", () -> "secret");
        registry.add("spring.cloud.vault.kv.default-context", () -> APP_SECRET_PATH);

        // Application configuration
        registry.add("spring.application.name", () -> "api-keys-vault-test");

        // Database (necesaria para que arranque Spring Boot)
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:apitest" + System.currentTimeMillis());
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Disable unnecessary features
        registry.add("spring.data.redis.enabled", () -> false);
        registry.add("spring.security.enabled", () -> false);

        // Actuator para testing
        registry.add("management.endpoints.web.exposure.include", () -> "health,info");
        registry.add("management.endpoint.health.enabled", () -> true);

        // Logging
        registry.add("logging.level.root", () -> "INFO");
        registry.add("logging.level.org.springframework.vault", () -> "DEBUG");

        System.out.println("✅ API Keys Test configurado completamente via @DynamicPropertySource");
    }

    @BeforeAll
    static void setupApiSecrets() throws Exception {
        System.out.println("🔧 Creando API secrets en Vault...");

        Map<String, Object> apiSecrets = Map.of(
                "app.api.external-service-key", "ext-svc-" + System.currentTimeMillis(),
                "app.api.payment-gateway-key", "pay-gw-" + System.currentTimeMillis(),
                "app.monitoring.datadog-key", "dd-" + System.currentTimeMillis(),
                "app.monitoring.new-relic-key", "nr-" + System.currentTimeMillis()
        );

        createSecretsInVault(apiSecrets);
        System.out.println("✅ API secrets listos en Vault");
    }

    @Test
    @DisplayName("🔑 API keys deben leerse desde Vault")
    void apiKeysShouldBeReadFromVault() {
        String externalKey = environment.getProperty("app.api.external-service-key");
        String paymentKey = environment.getProperty("app.api.payment-gateway-key");
        String datadogKey = environment.getProperty("app.monitoring.datadog-key");
        String newRelicKey = environment.getProperty("app.monitoring.new-relic-key");

        System.out.println("🔍 External Key: " + (externalKey != null ? externalKey.substring(0, 10) + "..." : "NULL"));
        System.out.println("🔍 Payment Key: " + (paymentKey != null ? paymentKey.substring(0, 10) + "..." : "NULL"));
        System.out.println("🔍 Datadog Key: " + (datadogKey != null ? datadogKey.substring(0, 10) + "..." : "NULL"));
        System.out.println("🔍 New Relic Key: " + (newRelicKey != null ? newRelicKey.substring(0, 10) + "..." : "NULL"));

        assertThat(externalKey).isNotNull();
        assertThat(externalKey).startsWith("ext-svc-");

        assertThat(paymentKey).isNotNull();
        assertThat(paymentKey).startsWith("pay-gw-");

        assertThat(datadogKey).isNotNull();
        assertThat(datadogKey).startsWith("dd-");

        assertThat(newRelicKey).isNotNull();
        assertThat(newRelicKey).startsWith("nr-");

        System.out.println("✅ API Keys verificadas desde Vault");
    }

    @Test
    @DisplayName("🏥 Aplicación debe funcionar con API keys desde Vault")
    void applicationShouldWorkWithApiKeysFromVault() {
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println("✅ Aplicación funcionando con API keys desde Vault");
    }

    private static void createSecretsInVault(Map<String, Object> secrets) throws Exception {
        String vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        String payload = objectMapper.writeValueAsString(Map.of("data", secrets));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", VAULT_ROOT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = vaultClient.exchange(
                vaultBaseUrl + "/v1/secret/data/" + APP_SECRET_PATH,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create secrets: " + response.getBody());
        }
    }

    @AfterAll
    static void cleanup() {
        System.out.println("🧹 API Keys Test completado");
    }
}
