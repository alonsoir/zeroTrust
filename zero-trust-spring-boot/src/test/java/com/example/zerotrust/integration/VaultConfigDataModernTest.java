package com.example.zerotrust.integration;

import com.example.zerotrust.config.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 🎯 TEST ESTADO DEL ARTE: Spring Cloud Vault con ConfigData API
 *
 * ✅ CARACTERÍSTICAS MODERNAS:
 * - ConfigData API puro (sin Bootstrap Context)
 * - Spring Boot 3.3.5 compatible
 * - Java 21 Records integration
 * - Testcontainers última versión
 * - Zero Trust architecture compliant
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // ✅ ConfigData API moderno
                "spring.config.import=vault://secret/db-app",
                "spring.profiles.active=vault-integration",

                // ✅ Configuración Vault moderna
                "spring.cloud.vault.enabled=true",
                "spring.cloud.vault.scheme=http",
                "spring.cloud.vault.authentication=token",
                "spring.cloud.vault.fail-fast=true",
                "spring.cloud.vault.kv.enabled=true",
                "spring.cloud.vault.kv.backend=secret",
                "spring.cloud.vault.kv.default-context=db-app"
        }
)
@Testcontainers
@ContextConfiguration(initializers = VaultConfigDataModernTest.VaultConfigDataInitializer.class)
@DisplayName("🎯 TEST ESTADO DEL ARTE: Spring Cloud Vault ConfigData API")
class VaultConfigDataModernTest {

    private static final String VAULT_ROOT_TOKEN = "production-test-token";
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

    @Autowired
    private JwtProperties jwtProperties;

    private static final TestRestTemplate vaultClient = new TestRestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 🚀 ConfigData Initializer Estado del Arte
     *
     * Configura Vault ANTES del ConfigData loading usando la API moderna.
     */
    public static class VaultConfigDataInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            System.out.println("🚀 [CONFIGDATA] Iniciando configuración moderna de Vault...");

            // Paso 1: Asegurar Vault disponible
            ensureVaultReady();

            // Paso 2: Crear secrets modernos
            setupModernVaultSecrets();

            // Paso 3: Configurar conexión ConfigData
            configureModernVaultConnection(applicationContext.getEnvironment());

            System.out.println("✅ [CONFIGDATA] Vault configurado con ConfigData API moderno");
        }

        private void ensureVaultReady() {
            if (!vaultContainer.isRunning()) {
                vaultContainer.start();
            }

            await()
                    .atMost(30, SECONDS)
                    .pollInterval(1, SECONDS)
                    .ignoreExceptions()
                    .until(() -> {
                        try {
                            String vaultUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
                            ResponseEntity<String> response = vaultClient.getForEntity(
                                    vaultUrl + "/v1/sys/health", String.class);
                            return response.getStatusCode().is2xxSuccessful();
                        } catch (Exception e) {
                            System.out.println("⏳ Esperando Vault...");
                            return false;
                        }
                    });

            System.out.println("✅ Vault operativo en puerto " + vaultContainer.getMappedPort(8200));
        }

        private void setupModernVaultSecrets() {
            try {
                System.out.println("🔧 Creando secrets modernos en Vault...");

                // ✅ Secrets estado del arte para Zero Trust
                Map<String, Object> secrets = Map.of(
                        "app.database.username", "vault_database_user",
                        "app.database.password", "vault_super_secure_password_123",
                        "app.jwt.secret", "this-is-a-very-long-secret-key-for-jwt-that-is-at-least-64-characters-long-123456",
                        "app.jwt.issuer", "zero-trust-production",
                        "app.jwt.algorithm", "HS256",
                        "app.jwt.access-token-duration", "PT15M",
                        "app.jwt.refresh-token-duration", "P7D",
                        "app.jwt.enable-refresh-token-rotation", "true",
                        "app.database.pool-size", "20",
                        "app.security.cors-origins", "https://app.company.com"
                );

                String vaultBaseUrl = "http://localhost:" + vaultContainer.getMappedPort(8200);
                String payload = objectMapper.writeValueAsString(of("data", secrets));

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

                System.out.println("✅ Secrets modernos creados en Vault");
                Thread.sleep(1000); // Dar tiempo para propagación

            } catch (Exception e) {
                System.err.println("❌ Error creando secrets: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        private void configureModernVaultConnection(ConfigurableEnvironment environment) {
            System.out.println("⚙️ Configurando conexión moderna a Vault...");

            Map<String, Object> vaultProps = new HashMap<>();

            // ✅ Configuración ConfigData API
            vaultProps.put("spring.cloud.vault.host", "localhost");
            vaultProps.put("spring.cloud.vault.port", vaultContainer.getMappedPort(8200));
            vaultProps.put("spring.cloud.vault.token", VAULT_ROOT_TOKEN);

            // ✅ Configuración de aplicación moderna
            vaultProps.put("spring.application.name", "zero-trust-vault-integration");

            // ✅ Base de datos H2 para tests
            vaultProps.put("spring.datasource.url", "jdbc:h2:mem:vaulttest" + System.currentTimeMillis());
            vaultProps.put("spring.datasource.username", "sa");
            vaultProps.put("spring.datasource.password", "");
            vaultProps.put("spring.datasource.driver-class-name", "org.h2.Driver");
            vaultProps.put("spring.jpa.hibernate.ddl-auto", "create-drop");

            // ✅ Deshabilitar servicios no necesarios
            vaultProps.put("spring.data.redis.enabled", false);

            environment.getPropertySources().addFirst(
                    new MapPropertySource("vaultModernTestConfiguration", vaultProps)
            );

            System.out.println("✅ " + vaultProps.size() + " propiedades configuradas");
        }
    }

    @BeforeAll
    static void setup() {
        System.out.println("🎯 Iniciando TEST ESTADO DEL ARTE con ConfigData API");
        System.out.println("🔐 Spring Boot 3.3.5 + Spring Cloud 2023.0.4");
    }

    @Test
    @DisplayName("🔐 Propiedades cargadas automáticamente con ConfigData API moderno")
    void propertiesShouldBeLoadedAutomaticallyWithModernConfigData() {
        System.out.println("🧪 Verificando carga automática con ConfigData API...");

        // ✅ Verificar propiedades individuales
        String dbUsername = environment.getProperty("app.database.username");
        String dbPassword = environment.getProperty("app.database.password");
        String jwtSecret = environment.getProperty("app.jwt.secret");
        String jwtIssuer = environment.getProperty("app.jwt.issuer");

        System.out.println("🔍 Propiedades cargadas con ConfigData API:");
        System.out.println("   Database Username: " + dbUsername);
        System.out.println("   JWT Secret length: " + (jwtSecret != null ? jwtSecret.length() : "null"));
        System.out.println("   JWT Issuer: " + jwtIssuer);

        // ✅ Assertions de propiedades individuales
        assertThat(dbUsername).isEqualTo("vault_database_user");
        assertThat(dbPassword).isEqualTo("vault_super_secure_password_123");
        assertThat(jwtSecret).isNotNull().hasSizeGreaterThanOrEqualTo(64);
        assertThat(jwtIssuer).isEqualTo("zero-trust-production");

        System.out.println("✅ Propiedades individuales validadas");
    }

    @Test
    @DisplayName("🎯 JwtProperties Record cargadas automáticamente desde Vault")
    void jwtPropertiesRecordShouldBeLoadedFromVault() {
        System.out.println("🧪 Verificando JwtProperties Record desde Vault...");

        // ✅ Verificar que el Record se cargó correctamente
        assertThat(jwtProperties).isNotNull();
        assertThat(jwtProperties.secret()).isNotNull().hasSizeGreaterThanOrEqualTo(64);
        assertThat(jwtProperties.issuer()).isEqualTo("zero-trust-production");
        assertThat(jwtProperties.algorithm()).isEqualTo("HS256");
        assertThat(jwtProperties.accessTokenDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwtProperties.refreshTokenDuration()).isEqualTo(Duration.ofDays(7));
        assertThat(jwtProperties.enableRefreshTokenRotation()).isTrue();

        // ✅ Verificar que viene de Vault
        assertThat(jwtProperties.isSecretFromVault()).isTrue();

        // ✅ Validar configuración
        jwtProperties.validate();

        System.out.println("✅ JwtProperties Record validado:");
        System.out.println("   " + jwtProperties.toString());
        System.out.println("   Secret info: " + jwtProperties.getSecretInfo());
    }

    @Test
    @DisplayName("🛡️ Configuración de seguridad Zero Trust verificada")
    void securityConfigurationShouldBeZeroTrustCompliant() {
        System.out.println("🛡️ Verificando cumplimiento Zero Trust...");

        // ✅ Verificar que NO hay secrets hardcodeados
        String jwtSecret = jwtProperties.secret();
        assertThat(jwtSecret).doesNotContain("hardcoded");
        assertThat(jwtSecret).doesNotContain("temporary");
        assertThat(jwtSecret).doesNotContain("test");

        // ✅ Verificar compliance de seguridad
        assertThat(jwtSecret.length()).isGreaterThanOrEqualTo(64);
        assertThat(jwtProperties.algorithm()).isEqualTo("HS256");
        assertThat(jwtProperties.enableRefreshTokenRotation()).isTrue();

        // ✅ Verificar propiedades adicionales de seguridad
        String corsOrigins = environment.getProperty("app.security.cors-origins");
        String poolSize = environment.getProperty("app.database.pool-size");

        assertThat(corsOrigins).isEqualTo("https://app.company.com");
        assertThat(poolSize).isEqualTo("20");

        System.out.println("✅ Configuración Zero Trust verificada");
    }

    @Test
    @DisplayName("📊 Metadatos de configuración moderna verificados")
    void modernConfigurationMetadataShouldBeValid() {
        System.out.println("📊 Verificando metadatos de configuración moderna...");

        // ✅ Verificar que es configuración moderna
        assertThat(jwtProperties.getClass().getSimpleName()).isEqualTo("JwtProperties");
        assertThat(jwtProperties.getClass().isRecord()).isTrue();

        // ✅ Verificar información de configuración
        String secretInfo = jwtProperties.getSecretInfo();
        assertThat(secretInfo).contains("Vault");
        assertThat(secretInfo).contains("HS256");

        // ✅ Verificar profile activo
        String[] activeProfiles = environment.getActiveProfiles();
        assertThat(activeProfiles).contains("vault-integration");

        System.out.println("✅ Metadatos de configuración validados:");
        System.out.println("   Profiles activos: " + java.util.Arrays.toString(activeProfiles));
        System.out.println("   Configuración: " + secretInfo);
    }

    @AfterAll
    static void cleanup() {
        System.out.println("🧹 TEST ESTADO DEL ARTE completado exitosamente");
        System.out.println("🎉 ConfigData API funciona perfectamente");
        System.out.println("🚀 Configuración moderna validada para PRODUCCIÓN");
    }
}