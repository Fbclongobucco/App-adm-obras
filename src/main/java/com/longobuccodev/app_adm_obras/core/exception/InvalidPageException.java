package com.longobuccodev.app_adm_obras.core.exception;

public class InvalidPageException extends CoreDomainException {

    private InvalidPageException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidPageException negativePage(int page) {
        return new InvalidPageException("page.invalid.number", "Page index must not be negative: " + page);
    }

    public static InvalidPageException invalidSize(int size) {
        return new InvalidPageException("page.invalid.size", "Page size must be between 1 and 100: " + size);
    }
}
