package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.exception.InvalidPageException;

import java.util.Locale;
import java.util.Set;

/** Ordenacao de uma listagem por um campo do dominio, restrito a uma lista permitida. */
public record Sort(String field, Direction direction) {

    public enum Direction {
        ASC, DESC
    }

    /**
     * Le o formato {@code campo,direcao}. Texto vazio ou nulo significa sem ordenacao.
     * Campo fora de {@code allowedFields} ou direcao diferente de asc/desc e rejeitado.
     */
    public static Sort parse(String text, Set<String> allowedFields) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String[] parts = text.split(",", -1);
        if (parts.length > 2) {
            throw InvalidPageException.invalidSort(text);
        }
        String field = parts[0].strip();
        if (!allowedFields.contains(field)) {
            throw InvalidPageException.invalidSort(text);
        }
        if (parts.length == 1) {
            return new Sort(field, Direction.ASC);
        }
        return switch (parts[1].strip().toLowerCase(Locale.ROOT)) {
            case "asc" -> new Sort(field, Direction.ASC);
            case "desc" -> new Sort(field, Direction.DESC);
            default -> throw InvalidPageException.invalidSort(text);
        };
    }
}
