package com.example.zerotrust.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PASO 1.5: Test intermedio - Spring Boot + Vault SIN bootstrap complexity
 *
 * ✅ Lo que funciona:
 * - TestContainers con Vault
 * - Spring Boot básico (sin Vault integration automática)
 * - Obtener secrets manualmente desde Spring Boot context
 * - Verificar que la aplicación funciona
 *
 * ❌ Lo que NO hacemos aún:
 * - Vault bootstrap automático
 * - @Value desde Vault (viene en Paso 3)
 */
@EnableAutoConfiguration(exclude = {
        org.springframework.cloud.vault.config.VaultAutoConfiguration.class
})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.vault.enabled=false",
                "spring.cloud.bootstrap.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.vault.config.lifecycle.enabled=false",
                // ✅ SOLUCIÓN: JWT secret con al menos 64 caracteres (elige una opción)

                // Opción 1: Descriptivo (80 caracteres)
                "app.jwt.secret=dummy-secret-for-step-1-5-with-enough-characters-to-pass-validation-check",

                // Opción 2: Simple con repetición (64 caracteres exactos)
                // "app.jwt.secret=dummy-secret-for-step-1-5-dummy-secret-for-step-1-5-dummy-12345",

                // Opción 3: Formato más realista (64 caracteres)
                // "app.jwt.secret=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
                "app.jwt.expiration=3600000",
                "app.jwt.issuer=step-1-5-test"
        }
)
@Testcontainers
@ActiveProfiles("step-1-5")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Step1_5_SpringBootVaultIntermediateTest extends BaseVaultIntegrationTest {

    @Container
    static GenericContainer<?> vaultContainer = createVaultContainer();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        // ✅ Base de datos simple
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:step15test");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");

        // ✅ JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // ✅ DESHABILITAR Security para este paso
        registry.add("spring.security.enabled", () -> false);
    }

    @BeforeAll
    static void setupVaultSecrets() throws Exception {
        vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);

        System.out.println("🔧 PASO 1.5: Preparando secrets en Vault...");

        // Crear secrets que luego podremos leer manualmente
        createStandardSecrets("step-1-5");

        System.out.println("✅ Secrets listos para PASO 1.5");
    }

    @Test
    @Order(1)
    @DisplayName("🔍 Spring Boot debe estar funcionando")
    void springBootShouldBeRunning() {
        // Verificar que Spring Boot está funcionando
        assertThat(applicationContext).isNotNull();

        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println("✅ Spring Boot está funcionando correctamente");
    }

    @Test
    @Order(2)
    @DisplayName("🔐 Vault debe estar funcionando")
    void vaultShouldBeRunning() {
        assertThat(vaultContainer.isRunning()).isTrue();

        ResponseEntity<String> response = vaultClient.getForEntity(
                vaultBaseUrl + "/v1/sys/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println("✅ Vault está funcionando en: " + vaultBaseUrl);
    }

    @Test
    @Order(3)
    @DisplayName("🔑 Debería obtener JWT secret de Vault manualmente")
    void shouldGetJwtSecretFromVaultManually() throws Exception {
        // Usar el método heredado para verificar que el secret existe
        verifySecretExists("step-1-5/jwt", "jwt-secret", "jwt-expiration", "jwt-issuer");

        // Leer el secret directamente desde Vault
        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/data/step-1-5/jwt",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        JsonNode data = responseBody.path("data").path("data");

        // Verificar que el secret está ahí
        assertThat(data.has("jwt-secret")).isTrue();
        String jwtSecret = data.get("jwt-secret").asText();
        assertThat(jwtSecret).startsWith("step-1-5-jwt-secret");

        System.out.println("✅ JWT Secret obtenido de Vault:");
        System.out.println("🔑 Secret: " + jwtSecret.substring(0, 20) + "...");
        System.out.println("⏰ Expiration: " + data.get("jwt-expiration").asText());
    }

    @Test
    @Order(4)
    @DisplayName("🗄️ Debería obtener credenciales DB de Vault manualmente")
    void shouldGetDatabaseCredentialsFromVaultManually() throws Exception {
        // Usar el método heredado
        verifySecretExists("step-1-5/database", "username", "password", "url");

        ResponseEntity<String> response = makeVaultRequest(
                "/v1/secret/data/step-1-5/database",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        JsonNode data = responseBody.path("data").path("data");

        // Verificar credenciales
        assertThat(data.has("username")).isTrue();
        assertThat(data.get("username").asText()).isEqualTo("step_1_5_user");

        assertThat(data.has("password")).isTrue();
        assertThat(data.get("password").asText()).startsWith("step-1-5-password");

        System.out.println("✅ Credenciales DB obtenidas de Vault:");
        System.out.println("👤 Username: " + data.get("username").asText());
        System.out.println("🔒 Password: " + data.get("password").asText().substring(0, 10) + "...");
    }

    @Test
    @Order(5)
    @DisplayName("🌍 Environment debe tener configuraciones básicas")
    void environmentShouldHaveBasicConfiguration() {
        // Verificar que el environment está bien configurado
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        assertThat(datasourceUrl).contains("jdbc:h2:mem:step15test");

        // Verificar que Vault está deshabilitado
        String vaultEnabled = environment.getProperty("spring.cloud.vault.enabled");
        assertThat(vaultEnabled).isEqualTo("false");

        // ✅ NUEVO: Verificar que las propiedades JWT dummy están configuradas
        String jwtSecret = environment.getProperty("app.jwt.secret");
        assertThat(jwtSecret).isEqualTo("dummy-secret-for-step-1-5-with-enough-characters-to-pass-validation-check");

        System.out.println("✅ Environment configurado correctamente:");
        System.out.println("📊 Datasource: " + datasourceUrl);
        System.out.println("🔐 Vault bootstrap: " + vaultEnabled);
        System.out.println("🔑 JWT Secret (dummy): " + jwtSecret);
    }

    @Test
    @Order(6)
    @DisplayName("🔄 Simular configuración manual desde Vault")
    void shouldSimulateManualConfigurationFromVault() throws Exception {
        // Simular lo que haríamos en una aplicación real:
        // 1. Obtener secrets de Vault
        // 2. Configurar manualmente nuestra aplicación

        // Obtener JWT secret
        ResponseEntity<String> jwtResponse = makeVaultRequest(
                "/v1/secret/data/step-1-5/jwt", HttpMethod.GET, null);
        JsonNode jwtData = objectMapper.readTree(jwtResponse.getBody()).path("data").path("data");

        // Obtener DB credentials
        ResponseEntity<String> dbResponse = makeVaultRequest(
                "/v1/secret/data/step-1-5/database", HttpMethod.GET, null);
        JsonNode dbData = objectMapper.readTree(dbResponse.getBody()).path("data").path("data");

        // Simular configuración de un service
        String configuredJwtSecret = jwtData.get("jwt-secret").asText();
        String configuredDbUser = dbData.get("username").asText();

        // En una aplicación real, aquí configuraríamos nuestros services
        assertThat(configuredJwtSecret).isNotNull();
        assertThat(configuredDbUser).isNotNull();

        System.out.println("✅ Configuración manual simulada:");
        System.out.println("🔑 JWT configurado: " + configuredJwtSecret.length() + " chars");
        System.out.println("👤 DB User configurado: " + configuredDbUser);
        System.out.println("💡 DIFERENCIA: En Paso 2 esto sería automático con @Value");
        System.out.println("💡 En Paso 1.5 lo hacemos manualmente como preparación");
    }

    @AfterAll
    static void cleanup() {
        System.out.println("🧹 PASO 1.5 completado - Spring Boot + Vault (manual)");
        System.out.println("➡️  Siguiente: PASO 2 - Spring Boot + Vault (automático)");
    }
}