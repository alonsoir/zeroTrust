package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test SIMPLE para empezar - Solo Vault, sin Spring Boot integration
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleVaultTokenTest {

    private static final String VAULT_ROOT_TOKEN = "simple-test-root-token";

    @Container
    static GenericContainer<?> vaultContainer = new GenericContainer<>(DockerImageName.parse("hashicorp/vault:1.15.4"))
            .withExposedPorts(8200)
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_ROOT_TOKEN)
            .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            .withCommand("vault", "server", "-dev")
            .waitingFor(Wait.forHttp("/v1/sys/health")
                    .forPort(8200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private static TestRestTemplate restTemplate = new TestRestTemplate();
    private static String vaultBaseUrl;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setup() {
        vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        System.out.println("🏗️ Vault iniciado en: " + vaultBaseUrl);
    }

    @Test
    @Order(1)
    @DisplayName("🔍 Vault debe estar funcionando")
    void vaultShouldBeUp() {
        assertThat(vaultContainer.isRunning()).isTrue();

        ResponseEntity<String> response = restTemplate.getForEntity(
                vaultBaseUrl + "/v1/sys/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Vault está corriendo correctamente");
    }

    @Test
    @Order(2)
    @DisplayName("🔐 Crear un JWT secret en Vault")
    void shouldCreateJwtSecret() throws Exception {
        // Crear secret JWT
        Map<String, Object> jwtData = Map.of(
                "secret", "mi-super-secreto-jwt-para-zero-trust-256-bits",
                "expiration", "3600000",
                "issuer", "zero-trust-app"
        );

        String payload = objectMapper.writeValueAsString(Map.of("data", jwtData));

        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/data/jwt",
                HttpMethod.POST,
                payload
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ JWT secret creado en Vault");
    }

    @Test
    @Order(3)
    @DisplayName("🔑 Obtener el JWT secret desde Vault")
    void shouldRetrieveJwtSecret() throws Exception {
        // Primero crear el secret
        shouldCreateJwtSecret();

        // Luego obtenerlo
        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/data/jwt",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        JsonNode data = responseBody.path("data").path("data");

        // Verificar el secret
        assertThat(data.has("secret")).isTrue();
        String secret = data.get("secret").asText();
        assertThat(secret).isEqualTo("mi-super-secreto-jwt-para-zero-trust-256-bits");

        System.out.println("✅ JWT secret obtenido exitosamente:");
        System.out.println("🔑 Secret: " + secret.substring(0, 20) + "...");
        System.out.println("⏰ Expiration: " + data.get("expiration").asText());
        System.out.println("🏢 Issuer: " + data.get("issuer").asText());
    }

    @Test
    @Order(4)
    @DisplayName("📝 Crear múltiples secrets para Zero Trust")
    void shouldCreateMultipleSecrets() throws Exception {
        // Secret para base de datos
        Map<String, Object> dbSecrets = Map.of(
                "username", "vault_db_user",
                "password", "vault_secure_db_password_123",
                "encryption-key", "AES-256-key-for-database-encryption"
        );

        // Secret para API keys
        Map<String, Object> apiSecrets = Map.of(
                "internal-service", "int-svc-key-" + System.currentTimeMillis(),
                "external-partner", "ext-partner-key-" + System.currentTimeMillis(),
                "monitoring", "monitoring-key-12345"
        );

        // Crear ambos secrets
        String dbPayload = objectMapper.writeValueAsString(Map.of("data", dbSecrets));
        String apiPayload = objectMapper.writeValueAsString(Map.of("data", apiSecrets));

        ResponseEntity<String> dbResponse = makeVaultRequest(
                "/v1/secret/data/database", HttpMethod.POST, dbPayload);
        ResponseEntity<String> apiResponse = makeVaultRequest(
                "/v1/secret/data/api-keys", HttpMethod.POST, apiPayload);

        assertThat(dbResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println("✅ Múltiples secrets creados para Zero Trust");
    }

    @Test
    @Order(5)
    @DisplayName("📋 Listar todos los secrets creados")
    void shouldListAllSecrets() throws Exception {
        // Crear los secrets primero
        shouldCreateMultipleSecrets();

        // Listar secrets
        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/metadata?list=true",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        JsonNode keys = responseBody.path("data").path("keys");

        System.out.println("📋 Secrets disponibles en Vault:");
        for (JsonNode key : keys) {
            System.out.println("  🔐 " + key.asText());
        }

        // Verificar que nuestros secrets están ahí
        boolean hasJwt = false, hasDb = false, hasApi = false;
        for (JsonNode key : keys) {
            String keyName = key.asText();
            if (keyName.equals("jwt")) hasJwt = true;
            if (keyName.equals("database")) hasDb = true;
            if (keyName.equals("api-keys")) hasApi = true;
        }

        assertThat(hasJwt).isTrue();
        assertThat(hasDb).isTrue();
        assertThat(hasApi).isTrue();

        System.out.println("✅ Todos los secrets están presentes");
    }

    /**
     * Helper para hacer requests a Vault
     */
    private ResponseEntity<String> makeVaultRequest(String path, HttpMethod method, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", VAULT_ROOT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                vaultBaseUrl + path,
                method,
                entity,
                String.class
        );
    }

    @AfterAll
    static void cleanup() {
        System.out.println("🧹 Test simple completado - Vault container se limpiará automáticamente");
    }
}