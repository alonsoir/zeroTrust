package com.example.zerotrust.integration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

public class VaultInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String vaultHost = environment.getProperty("spring.cloud.vault.host", "localhost");
        int vaultPort = Integer.parseInt(environment.getProperty("spring.cloud.vault.port", "8200"));
        String vaultToken = environment.getProperty("spring.cloud.vault.token", "db-test-token");
        String vaultPath = environment.getProperty("spring.cloud.vault.kv.default-context", "db-app");

        System.out.println("🔍 Initializing Vault: host=" + vaultHost + ", port=" + vaultPort + ", token=***, path=" + vaultPath);

        try {
            // Configurar el endpoint de Vault
            VaultEndpoint vaultEndpoint = VaultEndpoint.from(URI.create("http://" + vaultHost + ":" + vaultPort));
            ClientAuthentication authentication = new TokenAuthentication(vaultToken);

            // Crear VaultTemplate
            VaultTemplate vaultTemplate = new VaultTemplate(vaultEndpoint, authentication);
            System.out.println("🔗 Vault connection established to " + vaultEndpoint.getHost() + ":" + vaultEndpoint.getPort());

            // Leer los secretos
            VaultResponse response = vaultTemplate.read("secret/data/" + vaultPath);
            if (response != null && response.getData() != null) {
                Map<String, Object> secrets = response.getData();
                secrets.forEach((key, value) -> {
                    environment.getPropertySources().addFirst(new MapPropertySource("vault-override-" + key, Collections.singletonMap(key, value)));
                    System.out.println("🔐 Loaded secret: " + key + " = " + value);
                });
                System.out.println("🔐 Vault secrets loaded: " + secrets.keySet());
            } else {
                System.out.println("⚠️ No secrets loaded from Vault at path: secret/data/" + vaultPath);
            }
        } catch (Exception e) {
            System.err.println("❌ Error connecting to Vault: " + e.getMessage());
            e.printStackTrace();
        }
    }
}