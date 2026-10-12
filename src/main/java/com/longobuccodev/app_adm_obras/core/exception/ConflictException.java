package com.longobuccodev.app_adm_obras.core.exception;

/**
 * A operacao conflita com dados existentes: valor unico repetido ou exclusao de registro
 * que ainda tem dependentes. Nada e gravado.
 */
public class ConflictException extends RuntimeException {

    private final String errorCode;

    private ConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /** {@code <recurso>.duplicate.<campo>}, ex.: {@code employee.duplicate.cpf}. */
    public static ConflictException duplicate(String resource, String field, String value) {
        return new ConflictException(resource + ".duplicate." + field,
                "Another " + resource + " already uses this " + field + ": " + value);
    }

    /** {@code <recurso>.in_use}; a mensagem nomeia o vinculo que impede a exclusao. */
    public static ConflictException inUse(String resource, String dependency) {
        return new ConflictException(resource + ".in_use",
                "Cannot delete " + resource + ": it is still linked to " + dependency);
    }

    public static ConflictException of(String errorCode, String message) {
        return new ConflictException(errorCode, message);
    }
}
