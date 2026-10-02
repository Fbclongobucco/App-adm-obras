package com.longobuccodev.app_adm_obras.application.dto.common;

import java.time.Instant;

public record ErrorResponseDTO(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path
) {
}
