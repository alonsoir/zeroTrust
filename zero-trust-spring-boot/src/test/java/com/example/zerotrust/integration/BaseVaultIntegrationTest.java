package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

/**
 * Clase base para tests de integración con Vault
 * Proporciona funcionalidad común para todos los pasos de integración
 */
public abstract class BaseVaultIntegrationTest {

    protected static final String VAULT_ROOT_TOKEN = "base-vault-test-token";
    protected static final TestRestTemplate vaultClient = new TestRestTemplate();
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    protected static String vaultBaseUrl;

    /**
     * Crear un contenedor de Vault estándar
     */
    protected static GenericContainer<?> createVaultContainer() {
        return new GenericContainer<>(DockerImageName.parse("hashicorp/vault:1.15.4"))
                .withExposedPorts(8200)
                .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_ROOT_TOKEN)
                .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
                .withCommand("vault", "server", "-dev")
                .waitingFor(Wait.forHttp("/v1/sys/health")
                        .forPort(8200)
                        .withStartupTimeout(Duration.ofMinutes(2)));
    }

    /**
     * Helper para hacer requests a Vault
     */
    protected static ResponseEntity<String> makeVaultRequest(String path, HttpMethod method, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", VAULT_ROOT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return vaultClient.exchange(
                vaultBaseUrl + path,
                method,
                entity,
                String.class
        );
    }

    /**
     * Crear secrets estándar para tests
     */
    protected static void createStandardSecrets(String pathPrefix) throws Exception {
        // JWT secrets
        Map<String, Object> jwtSecrets = Map.of(
                "jwt-secret", pathPrefix + "-jwt-secret-" + System.currentTimeMillis(),
                "jwt-expiration", "7200000",
                "jwt-issuer", pathPrefix + "-zero-trust"
        );

        // Database secrets
        Map<String, Object> dbSecrets = Map.of(
                "username", pathPrefix.replace("-", "_") + "_user",
                "password", pathPrefix + "-password-" + System.currentTimeMillis(),
                "url", "jdbc:postgresql://db:5432/zerotrust"
        );

        String jwtPayload = objectMapper.writeValueAsString(Map.of("data", jwtSecrets));
        String dbPayload = objectMapper.writeValueAsString(Map.of("data", dbSecrets));

        ResponseEntity<String> jwtResponse = makeVaultRequest(
                "/v1/secret/data/" + pathPrefix + "/jwt", HttpMethod.POST, jwtPayload);
        ResponseEntity<String> dbResponse = makeVaultRequest(
                "/v1/secret/data/" + pathPrefix + "/database", HttpMethod.POST, dbPayload);

        if (!jwtResponse.getStatusCode().is2xxSuccessful() || !dbResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create secrets in Vault for " + pathPrefix);
        }

        System.out.println("✅ Secrets creados en Vault para " + pathPrefix);
    }

    /**
     * Verificar que un secret existe y tiene las claves esperadas
     */
    protected void verifySecretExists(String secretPath, String... expectedKeys) throws Exception {
        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/data/" + secretPath, HttpMethod.GET, null);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Secret not found at path: " + secretPath);
        }

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        JsonNode data = responseBody.path("data").path("data");

        for (String key : expectedKeys) {
            if (!data.has(key)) {
                throw new AssertionError("Secret missing key '" + key + "' at path: " + secretPath);
            }
        }
    }
}