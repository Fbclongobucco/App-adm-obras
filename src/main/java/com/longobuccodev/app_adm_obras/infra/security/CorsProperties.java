package com.longobuccodev.app_adm_obras.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                ? List.of("http://localhost:3000")
                : List.copyOf(allowedOrigins);
        // O cookie do refresh token exige credenciais, e credenciais exigem origens explicitas
        if (allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS must list explicit origins; wildcard '*' is not allowed");
        }
    }

    public boolean allows(String origin) {
        return origin != null && allowedOrigins.contains(origin);
    }
}
