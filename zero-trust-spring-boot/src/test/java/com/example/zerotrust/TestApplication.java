package com.example.zerotrust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.vault.config.VaultAutoConfiguration;
import org.springframework.cloud.vault.config.VaultReactiveAutoConfiguration;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@EnableAutoConfiguration(exclude = {
        // Estas SÍ se pueden excluir (son auto-configuraciones)
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        VaultAutoConfiguration.class,
        VaultReactiveAutoConfiguration.class
        // NO incluir VaultBootstrapConfiguration ni VaultBootstrapPropertySourceConfiguration
        // porque NO son auto-configuraciones y causan el error
})
@Profile("test")
public class TestApplication {

    public static void main(String[] args) {
        // Configurar propiedades para deshabilitar bootstrap
        System.setProperty("spring.cloud.bootstrap.enabled", "false");
        System.setProperty("spring.cloud.vault.enabled", "false");
        System.setProperty("spring.profiles.active", "test");

        SpringApplication.run(TestApplication.class, args);
    }
}