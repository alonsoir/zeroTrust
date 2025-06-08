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
 * 🎯 TEST 2: Database Properties desde Vault - COMPLETAMENTE INDEPENDIENTE
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Test 2: Database Properties desde Vault")
class DatabasePropertiesVaultTest {

    private static final String VAULT_ROOT_TOKEN = "db-test-token";
    private static final String APP_SECRET_PATH = "db-app";

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

    private static final TestRestTemplate vaultClient = new TestRestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 🎯 CONFIGURACIÓN COMPLETA PARA DATABASE TEST
     */
    @DynamicPropertySource
    static void configureDatabaseTest(DynamicPropertyRegistry registry) {
        System.out.println("🎯 Configurando Database Test - Completamente independiente");

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
        registry.add("spring.application.name", () -> "database-vault-test");

        // Database configuration
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:dbtest" + System.currentTimeMillis());
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");

        // Disable unnecessary features
        registry.add("spring.data.redis.enabled", () -> false);
        registry.add("spring.security.enabled", () -> false);

        // Logging
        registry.add("logging.level.root", () -> "INFO");
        registry.add("logging.level.org.springframework.vault", () -> "DEBUG");

        System.out.println("✅ Database Test configurado completamente via @DynamicPropertySource");
    }

    @BeforeAll
    static void setupDatabaseSecrets() throws Exception {
        System.out.println("🔧 Creando Database secrets en Vault...");

        Map<String, Object> dbSecrets = Map.of(
                "app.database.username", "vault_database_user",
                "app.database.password", "vault_super_secure_password_123",
                "app.database.url", "jdbc:postgresql://vault-db:5432/vaultdb"
        );

        createSecretsInVault(dbSecrets);
        System.out.println("✅ Database secrets listos en Vault");
    }

    @Test
    @DisplayName("🗄️ Database credentials deben leerse desde Vault")
    void databaseCredentialsShouldBeReadFromVault() {
        String dbUsername = environment.getProperty("app.database.username");
        String dbPassword = environment.getProperty("app.database.password");
        String dbUrl = environment.getProperty("app.database.url");

        System.out.println("🔍 DB Username: " + dbUsername);
        System.out.println("🔍 DB Password: " + (dbPassword != null ? dbPassword.substring(0, 5) + "..." : "NULL"));
        System.out.println("🔍 DB URL: " + dbUrl);

        assertThat(dbUsername).isEqualTo("vault_database_user");
        assertThat(dbPassword).isEqualTo("vault_super_secure_password_123");
        assertThat(dbUrl).isEqualTo("jdbc:postgresql://vault-db:5432/vaultdb");

        System.out.println("✅ Database credentials verificadas desde Vault");
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
        System.out.println("🧹 Database Test completado");
    }
}

