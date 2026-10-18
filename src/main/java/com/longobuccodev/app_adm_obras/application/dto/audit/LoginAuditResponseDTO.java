package com.longobuccodev.app_adm_obras.application.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record LoginAuditResponseDTO(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        Instant occurredAt,
        String ipAddress,
        String userAgent
) {
}
