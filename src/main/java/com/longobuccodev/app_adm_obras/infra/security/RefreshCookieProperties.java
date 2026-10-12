package com.longobuccodev.app_adm_obras.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties(prefix = "app.security.refresh-cookie")
public record RefreshCookieProperties(Boolean secure, String sameSite, Boolean partitioned) {

    static final String SAME_SITE_VARIABLE = "APP_REFRESH_COOKIE_SAME_SITE";
    static final String SECURE_VARIABLE = "APP_REFRESH_COOKIE_SECURE";

    public RefreshCookieProperties {
        secure = secure == null || secure;
        partitioned = partitioned != null && partitioned;
        sameSite = normalize(sameSite);
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalStateException("SameSite=None requires a Secure cookie: " + SAME_SITE_VARIABLE
                    + "=None needs " + SECURE_VARIABLE + "=true");
        }
    }

    private static String normalize(String sameSite) {
        if (sameSite == null || sameSite.isBlank()) {
            return "Strict";
        }
        return switch (sameSite.strip().toLowerCase(Locale.ROOT)) {
            case "strict" -> "Strict";
            case "lax" -> "Lax";
            case "none" -> "None";
            default -> throw new IllegalStateException(
                    SAME_SITE_VARIABLE + " must be Strict, Lax or None, got '" + sameSite + "'");
        };
    }
}
