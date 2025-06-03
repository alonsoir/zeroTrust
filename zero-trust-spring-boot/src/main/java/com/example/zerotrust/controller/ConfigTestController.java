package com.example.zerotrust.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ConfigTestController {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @GetMapping("/test/config")
    public Map<String, String> getConfig() {
        return Map.of(
                "jwtSecretPrefix", jwtSecret.substring(0, 20) + "...",
                "jwtSecretLength", String.valueOf(jwtSecret.length()),
                "source", jwtSecret.contains("vault") ? "probably-vault" : "probably-fallback"
        );
    }
}
