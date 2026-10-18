package com.longobuccodev.app_adm_obras.core.exception;

import java.time.Instant;

public class InvalidAuditFilterException extends CoreDomainException {

    private InvalidAuditFilterException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidAuditFilterException unknownEntityType(String entityType) {
        return new InvalidAuditFilterException("audit.invalid.entityType", "Unknown entity type: " + entityType);
    }

    public static InvalidAuditFilterException invalidPeriod(Instant from, Instant to) {
        return new InvalidAuditFilterException("audit.invalid.period",
                "Period start must not be after its end: from=" + from + ", to=" + to);
    }
}
