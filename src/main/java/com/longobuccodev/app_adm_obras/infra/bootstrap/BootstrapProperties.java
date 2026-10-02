package com.longobuccodev.app_adm_obras.infra.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(Admin admin) {

    public BootstrapProperties {
        admin = admin == null ? new Admin(null, null, null) : admin;
    }

    public record Admin(String name, String email, String password) {
    }
}
