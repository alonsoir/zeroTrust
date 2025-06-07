package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
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
 * Test específico para obtener y validar tokens desde Vault
 * Enfocado únicamente en la integración con Vault
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VaultTokenRetrievalTest {

    private static final String VAULT_ROOT_TOKEN = "vault-token-test-root";
    private static final String VAULT_IMAGE = "hashicorp/vault:1.15.4";

    @Container
    static GenericContainer<?> vaultContainer = new GenericContainer<>(DockerImageName.parse(VAULT_IMAGE))
            .withExposedPorts(8200)
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_ROOT_TOKEN)
            .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            .withEnv("VAULT_API_ADDR", "http://0.0.0.0:8200")
            .withCommand("vault", "server", "-dev")
            .waitingFor(Wait.forHttp("/v1/sys/health")
                    .forPort(8200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private TestRestTemplate restTemplate = new TestRestTemplate();
    private static String vaultBaseUrl;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void configureVaultProperties(DynamicPropertyRegistry registry) {
        vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        // Configuración mínima para Vault
        registry.add("spring.cloud.vault.enabled", () -> false); // Deshabilitado para este test específico
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @BeforeAll
    static void waitForVault() {
        // Esperar a que Vault esté completamente listo
        vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
        System.out.println("🏗️ Vault iniciado en: " + vaultBaseUrl);
    }

    @Test
    @Order(1)
    @DisplayName("🔍 Verificar que Vault está funcionando")
    void vaultShouldBeAccessible() {
        assertThat(vaultContainer.isRunning()).isTrue();

        // Verificar health endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                vaultBaseUrl + "/v1/sys/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Vault health check: OK");
    }

    @Test
    @Order(2)
    @DisplayName("🔐 Crear y obtener JWT secret desde Vault")
    void shouldStoreAndRetrieveJwtSecret() throws Exception {
        // 1. Crear el secret JWT en Vault
        String jwtSecret = "zero-trust-jwt-secret-256-bits-minimum-for-security-testing-purposes";

        Map<String, Object> secretData = Map.of(
                "jwt-secret", jwtSecret,
                "jwt-expiration", "3600000",
                "issuer", "zero-trust-app",
                "algorithm", "HS256"
        );

        String payload = objectMapper.writeValueAsString(Map.of("data", secretData));

        ResponseEntity<String> createResponse = makeVaultRequest(
                "/v1/secret/data/jwt-config",
                HttpMethod.POST,
                payload
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ JWT secret creado en Vault");

        // 2. Obtener el secret desde Vault
        ResponseEntity<String> getResponse = makeVaultRequest(
                "/v1/secret/data/jwt-config",
                HttpMethod.GET,
                null
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(getResponse.getBody());
        JsonNode data = responseBody.path("data").path("data");

        // 3. Validar que el secret es correcto
        assertThat(data.has("jwt-secret")).isTrue();
        assertThat(data.get("jwt-secret").asText()).isEqualTo(jwtSecret);
        assertThat(data.get("jwt-expiration").asText()).isEqualTo("3600000");
        assertThat(data.get("issuer").asText()).isEqualTo("zero-trust-app");

        System.out.println("✅ JWT secret obtenido correctamente desde Vault");
        System.out.println("🔑 Secret length: " + data.get("jwt-secret").asText().length() + " caracteres");
    }

    @Test
    @Order(3)
    @DisplayName("🗝️ Crear y obtener API keys desde Vault")
    void shouldStoreAndRetrieveApiKeys() throws Exception {
        // Crear múltiples API keys para diferentes servicios
        Map<String, Object> apiKeys = Map.of(
                "internal-service-key", "int-svc-key-" + System.currentTimeMillis(),
                "external-api-key", "ext-api-key-" + System.currentTimeMillis(),
                "monitoring-key", "mon-key-" + System.currentTimeMillis(),
                "created-at", System.currentTimeMillis()
        );

        String payload = objectMapper.writeValueAsString(Map.of("data", apiKeys));

        // Crear los API keys
        ResponseEntity<String> createResponse = makeVaultRequest(
                "/v1/secret/data/api-keys",
                HttpMethod.POST,
                payload
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Obtener los API keys
        ResponseEntity<String> getResponse = makeVaultRequest(
                "/v1/secret/data/api-keys",
                HttpMethod.GET,
                null
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(getResponse.getBody());
        JsonNode data = responseBody.path("data").path("data");

        // Validar todos los keys
        assertThat(data.has("internal-service-key")).isTrue();
        assertThat(data.has("external-api-key")).isTrue();
        assertThat(data.has("monitoring-key")).isTrue();
        assertThat(data.has("created-at")).isTrue();

        // Validar que los keys tienen el formato esperado
        assertThat(data.get("internal-service-key").asText()).startsWith("int-svc-key-");
        assertThat(data.get("external-api-key").asText()).startsWith("ext-api-key-");

        System.out.println("✅ API Keys creados y obtenidos correctamente");
        System.out.println("🔑 Internal Service Key: " + data.get("internal-service-key").asText());
    }

    @Test
    @Order(4)
    @DisplayName("🔒 Crear token de aplicación específico")
    void shouldCreateApplicationSpecificToken() throws Exception {
        // Crear una política específica para la aplicación
        String policyName = "zero-trust-app-policy";
        String policy = """
                path "secret/data/zero-trust-app/*" {
                    capabilities = ["read", "list"]
                }
                path "secret/data/jwt-config" {
                    capabilities = ["read"]
                }
                path "secret/data/api-keys" {
                    capabilities = ["read"]
                }
                """;

        Map<String, String> policyData = Map.of("policy", policy);
        String policyPayload = objectMapper.writeValueAsString(policyData);

        // Crear la política
        ResponseEntity<String> policyResponse = makeVaultRequest(
                "/v1/sys/policies/acl/" + policyName,
                HttpMethod.PUT,
                policyPayload
        );
        assertThat(policyResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Crear un token con esa política
        Map<String, Object> tokenData = Map.of(
                "policies", new String[]{policyName},
                "ttl", "1h",
                "renewable", true
        );

        String tokenPayload = objectMapper.writeValueAsString(tokenData);

        ResponseEntity<String> tokenResponse = makeVaultRequest(
                "/v1/auth/token/create",
                HttpMethod.POST,
                tokenPayload
        );

        assertThat(tokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode tokenResponseBody = objectMapper.readTree(tokenResponse.getBody());
        String applicationToken = tokenResponseBody.path("auth").path("client_token").asText();

        assertThat(applicationToken).isNotEmpty();
        assertThat(applicationToken).startsWith("hvs.");

        System.out.println("✅ Token de aplicación creado exitosamente");
        System.out.println("🎫 Token: " + applicationToken.substring(0, 10) + "...");

        // Probar el token creado leyendo un secret
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", applicationToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<String> testResponse = restTemplate.exchange(
                vaultBaseUrl + "/v1/secret/data/jwt-config",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(testResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Token de aplicación funciona correctamente");
    }

    @Test
    @Order(5)
    @DisplayName("📋 Listar todos los secrets disponibles")
    void shouldListAvailableSecrets() throws Exception {
        ResponseEntity<String> listResponse = makeVaultRequest(
                "/v1/secret/metadata?list=true",
                HttpMethod.GET,
                null
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(listResponse.getBody());
        JsonNode keys = responseBody.path("data").path("keys");

        assertThat(keys.isArray()).isTrue();

        System.out.println("📋 Secrets disponibles en Vault:");
        for (JsonNode key : keys) {
            System.out.println("  - " + key.asText());
        }

        // Verificar que nuestros secrets están presentes
        boolean hasJwtConfig = false;
        boolean hasApiKeys = false;

        for (JsonNode key : keys) {
            String keyName = key.asText();
            if (keyName.equals("jwt-config")) hasJwtConfig = true;
            if (keyName.equals("api-keys")) hasApiKeys = true;
        }

        assertThat(hasJwtConfig).isTrue();
        assertThat(hasApiKeys).isTrue();

        System.out.println("✅ Todos los secrets esperados están presentes");
    }

    /**
     * Helper method para hacer requests a Vault con autenticación
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
        System.out.println("🧹 Test de tokens completado");
    }
}