package com.example.zerotrust.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class VaultTestController {

    @Value("${vault.secret.jwt.secret:not-from-vault}")
    private String vaultSecret;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @GetMapping("/vault/test")
    public Map<String, Object> testVault() {
        return Map.of(
                "vaultSecretLength", vaultSecret.length(),
                "jwtSecretLength", jwtSecret.length(),
                "vaultSecretPrefix", vaultSecret.substring(0, Math.min(20, vaultSecret.length())) + "...",
                "jwtSecretPrefix", jwtSecret.substring(0, Math.min(20, jwtSecret.length())) + "...",
                "usingVault", jwtSecret.contains("vault")
        );
    }
}
