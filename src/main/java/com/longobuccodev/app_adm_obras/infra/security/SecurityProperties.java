package com.longobuccodev.app_adm_obras.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Admin admin, Operador operador) {

    public record Admin(String username, String password) {
    }

    public record Operador(String username, String password) {
    }
}
