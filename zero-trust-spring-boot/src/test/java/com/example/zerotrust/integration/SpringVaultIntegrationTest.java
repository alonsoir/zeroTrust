package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
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
 * PASO 2: Test de integración donde Spring Boot lee automáticamente desde Vault
 *
 * Lo que probamos:
 * 1. ✅ Spring Boot se conecta a Vault automáticamente
 * 2. ✅ Lee secrets de Vault como properties (@Value)
 * 3. ✅ Environment contiene valores de Vault
 * 4. ✅ Aplicación funciona con secrets reales
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("vault-integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringVaultIntegrationTest {

    private static final String VAULT_ROOT_TOKEN = "spring-integration-root-token";
    private static final String APP_SECRET_PATH = "zero-trust-app";

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
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    // ✅ Estos valores deberían venir automáticamente de Vault
    @Value("${app.jwt.secret:NOT_FOUND}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:0}")
    private Long jwtExpiration;

    @Value("${app.database.username:NOT_FOUND}")
    private String dbUsername;

    private static String vaultBaseUrl;
    private static final TestRestTemplate vaultClient = new TestRestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * MÉTODO ESTÁNDAR para configurar Vault en tests de integración
     * Elimina la necesidad de archivos bootstrap y configuraciones complejas
     *
     * USO: Copiar este método en cualquier test que necesite Vault
     */
    @DynamicPropertySource
    static void configureVaultIntegrationTest(DynamicPropertyRegistry registry) {
        // URL dinámico del contenedor Vault
        String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        // ❌ DESHABILITAR Bootstrap completamente
        registry.add("spring.cloud.bootstrap.enabled", () -> false);

        // ✅ CONFIGURACIÓN COMPLETA DE VAULT
        registry.add("spring.cloud.vault.enabled", () -> true);
        registry.add("spring.cloud.vault.config.enabled", () -> true);
        registry.add("spring.cloud.vault.host", () -> "localhost");
        registry.add("spring.cloud.vault.port", () -> vaultContainer.getMappedPort(8200));
        registry.add("spring.cloud.vault.scheme", () -> "http");
        registry.add("spring.cloud.vault.token", () -> VAULT_ROOT_TOKEN);
        registry.add("spring.cloud.vault.authentication", () -> "token");

        // ✅ CONFIGURACIÓN KV STORE
        registry.add("spring.cloud.vault.kv.enabled", () -> true);
        registry.add("spring.cloud.vault.kv.backend", () -> "secret");
        registry.add("spring.cloud.vault.kv.default-context", () -> APP_SECRET_PATH);
        registry.add("spring.cloud.vault.kv.profile-separator", () -> "/");

        // ✅ TIMEOUTS Y RELIABILITY
        registry.add("spring.cloud.vault.fail-fast", () -> false);
        registry.add("spring.cloud.vault.connection-timeout", () -> 5000);
        registry.add("spring.cloud.vault.read-timeout", () -> 15000);

        // ✅ MONITORING
        registry.add("management.health.vault.enabled", () -> true);

        // ✅ BASE DE DATOS H2 PARA TESTS
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:vaulttest" + System.currentTimeMillis());
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");

        // ✅ JPA CONFIGURATION
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.show-sql", () -> false);

        // ✅ DESHABILITAR FUNCIONALIDADES QUE NO NECESITAMOS
        registry.add("spring.data.redis.enabled", () -> false);
        registry.add("spring.security.enabled", () -> false);

        // ✅ LOGGING PARA DEBUG
        registry.add("logging.level.org.springframework.vault", () -> "DEBUG");
        registry.add("logging.level.org.springframework.cloud.vault", () -> "DEBUG");
        registry.add("logging.level.org.springframework.cloud.bootstrap", () -> "ERROR");
        registry.add("logging.level.org.hibernate", () -> "WARN");
        registry.add("logging.level.org.testcontainers", () -> "INFO");

        System.out.println("🔧 Vault configurado dinámicamente: " + vaultUrl);
    }

    @BeforeAll
    static void setupVaultSecrets() throws Exception {
        // Esperar a que Vault esté listo
        vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        // ✅ IMPORTANTE: Crear los secrets ANTES de que Spring Boot inicie
        System.out.println("🔧 Preparando secrets en Vault para Spring Boot...");

        createSecretsInVault();

        System.out.println("✅ Secrets listos en Vault");
    }

    @Test
    @Order(1)
    @DisplayName("🔍 Vault debe estar funcionando")
    void vaultShouldBeRunning() {
        assertThat(vaultContainer.isRunning()).isTrue();

        ResponseEntity<String> response = vaultClient.getForEntity(
                vaultBaseUrl + "/v1/sys/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println("✅ Vault está corriendo en: " + vaultBaseUrl);
    }

    @Test
    @Order(2)
    @DisplayName("🌱 Spring Boot debe leer JWT secret desde Vault")
    void springBootShouldReadJwtSecretFromVault() {
        // ✅ Verificar que Spring Boot leyó el secret automáticamente
        assertThat(jwtSecret).isNotEqualTo("NOT_FOUND");
        assertThat(jwtSecret).isEqualTo("spring-boot-jwt-secret-from-vault-256-bits-minimum");
        assertThat(jwtExpiration).isEqualTo(7200000L);

        System.out.println("✅ JWT Secret leído desde Vault: " + jwtSecret.substring(0, 20) + "...");
        System.out.println("⏰ JWT Expiration: " + jwtExpiration + "ms");
    }

    @Test
    @Order(3)
    @DisplayName("🗄️ Spring Boot debe leer credenciales DB desde Vault")
    void springBootShouldReadDatabaseCredentialsFromVault() {
        // ✅ Verificar credenciales de base de datos
        assertThat(dbUsername).isNotEqualTo("NOT_FOUND");
        assertThat(dbUsername).isEqualTo("vault_db_user");

        System.out.println("✅ DB Username desde Vault: " + dbUsername);
    }

    @Test
    @Order(4)
    @DisplayName("🌍 Environment debe contener propiedades de Vault")
    void environmentShouldContainVaultProperties() {
        // ✅ Verificar que las propiedades están en el Environment
        String jwtFromEnv = environment.getProperty("app.jwt.secret");
        String dbUserFromEnv = environment.getProperty("app.database.username");

        assertThat(jwtFromEnv).isNotNull();
        assertThat(jwtFromEnv).isEqualTo("spring-boot-jwt-secret-from-vault-256-bits-minimum");

        assertThat(dbUserFromEnv).isNotNull();
        assertThat(dbUserFromEnv).isEqualTo("vault_db_user");

        System.out.println("✅ Environment properties desde Vault verificadas");
    }

    @Test
    @Order(5)
    @DisplayName("🏥 Aplicación debe estar funcionando con secrets de Vault")
    void applicationShouldWorkWithVaultSecrets() {
        // ✅ Verificar que la aplicación está funcionando
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ✅ Verificar que hay endpoints disponibles
        ResponseEntity<String> infoResponse = restTemplate.getForEntity("/actuator/info", String.class);
        // Puede ser 200 o 404, pero la aplicación debe responder
        assertThat(infoResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);

        System.out.println("✅ Aplicación funcionando correctamente con secrets de Vault");
    }

    @Test
    @Order(6)
    @DisplayName("🔄 Debe poder leer múltiples secrets de Vault")
    void shouldReadMultipleSecretsFromVault() {
        // ✅ Verificar que podemos acceder a múltiples secrets
        String apiKey = environment.getProperty("app.api.external-key");
        String monitoringKey = environment.getProperty("app.monitoring.key");

        // Estos deberían venir de Vault también
        assertThat(apiKey).isNotNull();
        assertThat(monitoringKey).isNotNull();

        System.out.println("✅ Múltiples secrets leídos desde Vault:");
        System.out.println("  🔑 API Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "NULL"));
        System.out.println("  📊 Monitoring Key: " + (monitoringKey != null ? monitoringKey.substring(0, 10) + "..." : "NULL"));
    }

    /**
     * ✅ Crear todos los secrets que Spring Boot necesita
     */
    private static void createSecretsInVault() throws Exception {
        // Secrets principales de la aplicación
        Map<String, Object> appSecrets = Map.of(
                "app.jwt.secret", "spring-boot-jwt-secret-from-vault-256-bits-minimum",
                "app.jwt.expiration", "7200000",
                "app.jwt.issuer", "zero-trust-vault-app",
                "app.database.username", "vault_db_user",
                "app.database.password", "vault_secure_password_123",
                "app.api.external-key", "ext-api-" + System.currentTimeMillis(),
                "app.monitoring.key", "monitoring-" + System.currentTimeMillis()
        );

        String payload = objectMapper.writeValueAsString(Map.of("data", appSecrets));

        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/data/" + APP_SECRET_PATH,
                HttpMethod.POST,
                payload
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create secrets in Vault: " + response.getBody());
        }

        System.out.println("✅ Secrets creados en path: /secret/data/" + APP_SECRET_PATH);
    }

    /**
     * Helper para hacer requests a Vault
     */
    private static ResponseEntity<String> makeVaultRequest(String path, HttpMethod method, String body) {
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

    @AfterAll
    static void cleanup() {
        System.out.println("🧹 Paso 2 completado - Spring Boot integrado con Vault");
    }
}