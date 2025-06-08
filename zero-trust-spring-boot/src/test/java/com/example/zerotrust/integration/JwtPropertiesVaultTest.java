package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🎯 TEST JWT + VAULT - SOLO @DynamicPropertySource
 *
 * ✅ Control total de propiedades via @DynamicPropertySource
 * ✅ NO usa archivos yml/properties externos
 * ✅ Fácil experimentación y debugging de propiedades
 * ✅ Identifica exactamente qué propiedades son necesarias
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("🔐 JWT Properties - Control Total via @DynamicPropertySource")
class JwtPropertiesVaultTest {

    private static final String VAULT_ROOT_TOKEN = "jwt-test-token-dynamic";
    private static final String APP_SECRET_PATH = "jwt-app-dynamic";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final WebClient vaultClient = WebClient.builder().build();

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

    /**
     * 🎯 CONTROL TOTAL DE PROPIEDADES - Solo @DynamicPropertySource
     */
    @DynamicPropertySource
    static void configureAllPropertiesViaDynamic(DynamicPropertyRegistry registry) {
        System.out.println("🎯 Configurando TODAS las propiedades via @DynamicPropertySource");

        // Spring Boot Core
        registry.add("spring.application.name", () -> "jwt-vault-dynamic-test");
        registry.add("server.port", () -> "0");

        // Disable Spring Cloud Vault
        registry.add("spring.cloud.vault.enabled", () -> "false");
        registry.add("spring.cloud.vault.config.enabled", () -> "false");
        registry.add("spring.cloud.bootstrap.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");

        // H2 Database
        String dbUrl = "jdbc:h2:mem:jwtdynamic" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        registry.add("spring.datasource.url", () -> dbUrl);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");

        // JPA/Hibernate
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");

        // Disable Redis
        registry.add("spring.data.redis.enabled", () -> "false");
        registry.add("spring.redis.enabled", () -> "false");
        registry.add("spring.cache.type", () -> "simple");

        // Security
        registry.add("spring.security.user.name", () -> "test");
        registry.add("spring.security.user.password", () -> "test");

        // Actuator
        registry.add("management.endpoints.web.exposure.include", () -> "health,info");
        registry.add("management.endpoint.health.enabled", () -> "true");

        // Logging
        registry.add("logging.level.root", () -> "INFO");
        registry.add("logging.level.com.example.zerotrust", () -> "DEBUG");
        registry.add("logging.level.org.springframework.vault", () -> "OFF");

        // Vault Secrets
        System.out.println("📋 Configurando Vault secrets...");
        Map<String, Object> secrets = readSecretsViaRest();
        secrets.forEach((key, value) -> {
            registry.add(key, value::toString);
            System.out.println("✅ Injected: " + key + " = " + (key.contains("secret") ? "[HIDDEN]" : value));
        });
    }

    // ============================================================================
    // TESTS
    // ============================================================================

    @Test
    @DisplayName("🌱 JWT Properties desde Vault via REST")
    void jwtPropertiesShouldBeReadFromVaultViaRest() {
        String jwtSecret = environment.getProperty("app.jwt.secret");
        String jwtExpiration = environment.getProperty("app.jwt.expiration");
        String jwtIssuer = environment.getProperty("app.jwt.issuer");

        assertThat(jwtSecret).isNotNull().hasSizeGreaterThan(64);
        assertThat(jwtExpiration).isEqualTo("7200000");
        assertThat(jwtIssuer).isEqualTo("jwt-rest-test-issuer");
    }

    @Test
    @DisplayName("🗄️ Database H2 configuración completa")
    void h2DatabaseShouldBeFullyConfigured() {
        String dataSourceUrl = environment.getProperty("spring.datasource.url");
        String dataSourceDriver = environment.getProperty("spring.datasource.driver-class-name");

        assertThat(dataSourceUrl).contains("h2:mem:jwt");
        assertThat(dataSourceDriver).isEqualTo("org.h2.Driver");
    }

    @Test
    @DisplayName("☁️ Spring Cloud debe estar completamente deshabilitado")
    void springCloudShouldBeCompletelyDisabled() {
        assertThat(environment.getProperty("spring.cloud.vault.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("spring.cloud.bootstrap.enabled")).isEqualTo("false");
    }

    // ============================================================================
    // VAULT METHODS
    // ============================================================================

    private static void createSecretsInVault() {
        String vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        Map<String, Object> jwtSecrets = Map.of(
                "app.jwt.secret", "jwt-secret-from-vault-via-rest-at-least-64-characters-long-for-security",
                "app.jwt.expiration", "7200000",
                "app.jwt.issuer", "jwt-rest-test-issuer"
        );

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("data", jwtSecrets));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize secrets payload", e);
        }

        vaultClient.post()
                .uri(vaultBaseUrl + "/v1/secret/data/" + APP_SECRET_PATH)
                .header("X-Vault-Token", VAULT_ROOT_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .blockOptional()
                .orElseThrow(() -> new RuntimeException("Failed to create secrets in Vault"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readSecretsViaRest() {
        String vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        String response = vaultClient.get()
                .uri(vaultBaseUrl + "/v1/secret/data/" + APP_SECRET_PATH)
                .header("X-Vault-Token", VAULT_ROOT_TOKEN)
                .retrieve()
                .bodyToMono(String.class)
                .blockOptional()
                .orElseThrow(() -> new RuntimeException("Failed to read secrets from Vault"));

        try {
            Map<String, Object> responseBody = objectMapper.readValue(response, Map.class);
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            return (Map<String, Object>) data.get("data");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Vault secrets", e);
        }
    }

    @BeforeAll
    static void setupVault() {
        System.out.println("🚀 Iniciando Vault setup...");
        createSecretsInVault();
    }

    @AfterAll
    static void cleanup() {
        System.out.println("🗑️ Test cleanup completed");
    }
}