package com.longobuccodev.app_adm_obras.core.security;

import com.longobuccodev.app_adm_obras.core.domain.User;

import java.time.Instant;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String value, Instant issuedAt, Instant expiresAt) {
    }
}
