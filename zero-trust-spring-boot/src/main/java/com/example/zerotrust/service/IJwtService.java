package com.example.zerotrust.service;

import com.example.zerotrust.model.dto.security.JwtClaims;
import com.example.zerotrust.model.dto.security.TokenResponse;

/**
 * Contrato para el servicio JWT Zero Trust
 */
public interface IJwtService {

    /**
     * Genera un par de tokens (access + refresh) para el usuario
     */
    TokenResponse generateTokenPair(JwtClaims userClaims);

    /**
     * Genera solo access token (para refresh)
     */
    String generateAccessToken(JwtClaims claims);

    /**
     * Valida y parsea un token JWT
     */
    JwtClaims validateAndParseToken(String token);

    /**
     * Refresca un access token usando refresh token
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * Revoca un token (lo añade a blacklist)
     */
    void revokeToken(String token, String reason);

    /**
     * Extrae el token ID (jti) de un token
     */
    String extractTokenId(String token);
}