package com.longobuccodev.app_adm_obras.core.exception;

import java.time.Instant;

public class InvalidAuditStampException extends CoreDomainException {

    private InvalidAuditStampException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidAuditStampException missingCreatedAt() {
        return new InvalidAuditStampException("audit.invalid.created_at", "Creation timestamp must not be null");
    }

    public static InvalidAuditStampException missingUpdatedAt() {
        return new InvalidAuditStampException("audit.invalid.updated_at", "Update timestamp must not be null");
    }

    public static InvalidAuditStampException updatedBeforeCreated(Instant createdAt, Instant updatedAt) {
        return new InvalidAuditStampException("audit.invalid.updated_at",
                "Update timestamp " + updatedAt + " must not be before creation timestamp " + createdAt);
    }
}
