package com.longobuccodev.app_adm_obras.core.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Login com sucesso: quem entrou, quando, de qual IP e com qual user-agent. Nome e e-mail sao os do
 * instante do login. IP e user-agent podem faltar; valores longos sao truncados, nao rejeitados.
 */
public final class LoginRecord {

    public static final int MAX_IP_LENGTH = 45;
    public static final int MAX_USER_AGENT_LENGTH = 512;

    private final UUID id;
    private final UUID userId;
    private final String userName;
    private final String userEmail;
    private final Instant occurredAt;
    private final String ipAddress;
    private final String userAgent;

    public LoginRecord(UUID id, UUID userId, String userName, String userEmail, Instant occurredAt,
                       String ipAddress, String userAgent) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.userName = Objects.requireNonNull(userName, "userName");
        this.userEmail = Objects.requireNonNull(userEmail, "userEmail");
        // Mesma precisao do banco (timestamp(6))
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt").truncatedTo(ChronoUnit.MICROS);
        this.ipAddress = truncate(ipAddress, MAX_IP_LENGTH);
        this.userAgent = truncate(userAgent, MAX_USER_AGENT_LENGTH);
    }

    public static LoginRecord of(User user, Instant occurredAt, String ipAddress, String userAgent) {
        return new LoginRecord(UUID.randomUUID(), user.getId(), user.getName(), user.getEmail(), occurredAt,
                ipAddress, userAgent);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
