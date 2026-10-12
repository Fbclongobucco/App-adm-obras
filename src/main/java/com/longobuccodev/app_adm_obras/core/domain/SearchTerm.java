package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidPageException;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Termo de busca das listagens. Normaliza para comparacao sem acentos e sem diferenciar
 * maiusculas: {@link #text()} vai para um LIKE com escape '\', {@link #digits()} para CPF e CNPJ.
 */
public final class SearchTerm {

    public static final int MAX_LENGTH = 100;
    public static final char ESCAPE = '\\';
    public static final SearchTerm NONE = new SearchTerm("", "");

    private final String text;
    private final String digits;

    private SearchTerm(String text, String digits) {
        this.text = text;
        this.digits = digits;
    }

    public static SearchTerm of(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        String trimmed = raw.strip();
        if (trimmed.length() > MAX_LENGTH) {
            throw InvalidPageException.invalidQuery(trimmed.length());
        }
        String plain = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        String escaped = plain.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return new SearchTerm(escaped, trimmed.replaceAll("\\D", ""));
    }

    public boolean isNone() {
        return text.isEmpty();
    }

    /** Minusculas, sem acentos, com '\', '%' e '_' escapados. */
    public String text() {
        return text;
    }

    /** Apenas os digitos do termo; vazio quando nao ha digitos. */
    public String digits() {
        return digits;
    }
}
