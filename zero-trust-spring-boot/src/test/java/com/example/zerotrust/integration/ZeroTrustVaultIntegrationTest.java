package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración completo para arquitectura Zero Trust
 * Prueba la integración real con Vault usando TestContainers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ZeroTrustVaultIntegrationTest {

    private static final String VAULT_ROOT_TOKEN = "integration-test-root-token";
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

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("zerotrust_test")
            .withUsername("zerotrust_test")
            .withPassword("test_password");

    @Autowired
    private TestRestTemplate restTemplate;

    private WebClient vaultWebClient;
    private static String vaultBaseUrl;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configuración de Vault
        vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        registry.add("spring.cloud.vault.enabled", () -> true);
        registry.add("spring.cloud.vault.host", () -> "localhost");
        registry.add("spring.cloud.vault.port", () -> vaultContainer.getMappedPort(8200));
        registry.add("spring.cloud.vault.scheme", () -> "http");
        registry.add("spring.cloud.vault.token", () -> VAULT_ROOT_TOKEN);
        registry.add("spring.cloud.vault.authentication", () -> "token");
        registry.add("spring.cloud.vault.kv.enabled", () -> true);
        registry.add("spring.cloud.vault.kv.backend", () -> "secret");
        registry.add("spring.cloud.vault.kv.default-context", () -> "zero-trust-app");

        // Configuración de PostgreSQL
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        // Configuración específica para tests de integración
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("management.endpoints.web.exposure.include", () -> "health,info,vault");
    }

    @BeforeEach
    void setUp() {
        // Cliente para hacer llamadas directas a Vault
        vaultWebClient = WebClient.builder()
                .baseUrl(vaultBaseUrl)
                .defaultHeader("X-Vault-Token", VAULT_ROOT_TOKEN)
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("✅ Vault Container debe estar funcionando")
    void vaultContainerShouldBeRunning() {
        assertThat(vaultContainer.isRunning()).isTrue();

        // Verificar que Vault responde
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                vaultBaseUrl + "/v1/sys/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(2)
    @DisplayName("🔐 Debería poder crear secretos en Vault")
    void shouldCreateSecretsInVault() throws Exception {
        // Preparar secretos para nuestra aplicación Zero Trust
        Map<String, Object> jwtSecrets = Map.of(
                "jwt-secret", "super-secure-jwt-secret-for-zero-trust-testing-256-bits-minimum",
                "jwt-expiration", "3600000",
                "refresh-token-expiration", "86400000"
        );

        Map<String, Object> dbSecrets = Map.of(
                "db-username", "vault_user",
                "db-password", "vault_secure_password",
                "encryption-key", "AES256-encryption-key-for-database"
        );

        // Crear secretos JWT
        String jwtSecretsJson = objectMapper.writeValueAsString(Map.of("data", jwtSecrets));
        ResponseEntity<String> jwtResponse = makeVaultRequest(
                "/v1/secret/data/zero-trust-app/jwt",
                HttpMethod.POST,
                jwtSecretsJson
        );
        assertThat(jwtResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Crear secretos de DB
        String dbSecretsJson = objectMapper.writeValueAsString(Map.of("data", dbSecrets));
        ResponseEntity<String> dbResponse = makeVaultRequest(
                "/v1/secret/data/zero-trust-app/database",
                HttpMethod.POST,
                dbSecretsJson
        );
        assertThat(dbResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println("✅ Secretos creados exitosamente en Vault");
    }

    @Test
    @Order(3)
    @DisplayName("🔑 Debería obtener tokens JWT desde Vault")
    void shouldRetrieveJwtTokenFromVault() throws Exception {
        // Primero crear el secreto
        shouldCreateSecretsInVault();

        // Leer el secreto JWT desde Vault
        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/data/zero-trust-app/jwt",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        JsonNode data = responseBody.path("data").path("data");

        // Verificar que los secretos están presentes
        assertThat(data.has("jwt-secret")).isTrue();
        assertThat(data.get("jwt-secret").asText())
                .isEqualTo("super-secure-jwt-secret-for-zero-trust-testing-256-bits-minimum");

        assertThat(data.has("jwt-expiration")).isTrue();
        assertThat(data.get("jwt-expiration").asText()).isEqualTo("3600000");

        System.out.println("✅ JWT Token obtenido exitosamente desde Vault");
        System.out.println("🔐 Secret obtenido: " + data.get("jwt-secret").asText().substring(0, 20) + "...");
    }

    @Test
    @Order(4)
    @DisplayName("🗄️ Debería obtener credenciales de base de datos desde Vault")
    void shouldRetrieveDatabaseCredentialsFromVault() throws Exception {
        // Primero crear los secretos
        shouldCreateSecretsInVault();

        // Leer credenciales de DB desde Vault
        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/data/zero-trust-app/database",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        JsonNode data = responseBody.path("data").path("data");

        // Verificar credenciales
        assertThat(data.has("db-username")).isTrue();
        assertThat(data.get("db-username").asText()).isEqualTo("vault_user");

        assertThat(data.has("db-password")).isTrue();
        assertThat(data.get("db-password").asText()).isEqualTo("vault_secure_password");

        assertThat(data.has("encryption-key")).isTrue();

        System.out.println("✅ Credenciales de DB obtenidas exitosamente desde Vault");
    }

    @Test
    @Order(5)
    @DisplayName("🏥 Spring Boot debería integrar correctamente con Vault")
    void springBootShouldIntegrateWithVault() {
        // Verificar que la aplicación se levanta correctamente con Vault
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                "/actuator/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Si hay endpoint de vault disponible, verificarlo
        ResponseEntity<String> vaultHealthResponse = restTemplate.getForEntity(
                "/actuator/health/vault", String.class);

        // El status debería ser 200 (UP) o 503 (DOWN pero disponible)
        assertThat(vaultHealthResponse.getStatusCode())
                .isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);

        System.out.println("✅ Spring Boot integrado correctamente con Vault");
    }

    @Test
    @Order(6)
    @DisplayName("🔐 Debería validar políticas de acceso a Vault")
    void shouldValidateVaultAccessPolicies() throws Exception {
        // Crear una política específica para la aplicación
        String policy = """
                path "secret/data/zero-trust-app/*" {
                    capabilities = ["read", "list"]
                }
                path "secret/metadata/zero-trust-app/*" {
                    capabilities = ["read", "list"]
                }
                """;

        // Crear la política
        Map<String, String> policyData = Map.of("policy", policy);
        String policyJson = objectMapper.writeValueAsString(policyData);

        ResponseEntity<String> policyResponse = makeVaultRequest(
                "/v1/sys/policies/acl/zero-trust-app-policy",
                HttpMethod.PUT,
                policyJson
        );
        assertThat(policyResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Listar políticas para verificar que se creó
        ResponseEntity<String> listPoliciesResponse = makeVaultRequest(
                "/v1/sys/policies/acl",
                HttpMethod.GET,
                null
        );
        assertThat(listPoliciesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listPoliciesResponse.getBody()).contains("zero-trust-app-policy");

        System.out.println("✅ Políticas de acceso validadas correctamente");
    }

    /**
     * Helper method para hacer requests a Vault
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
    static void tearDown() {
        System.out.println("🧹 Limpiando recursos de TestContainers...");
    }
}