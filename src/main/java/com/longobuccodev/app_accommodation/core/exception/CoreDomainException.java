package com.longobuccodev.app_accommodation.core.exception;

public abstract class CoreDomainException extends RuntimeException {

    private final String errorCode;

    protected CoreDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}