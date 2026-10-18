package com.longobuccodev.app_adm_obras.support;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Conta os SELECT que o Hibernate prepara. Registrado por
 * {@code spring.jpa.properties.hibernate.session_factory.statement_inspector}; o Hibernate instancia a
 * classe, por isso o estado e estatico. So conta leituras: INSERT de vinculos cresce com a lista por
 * natureza e nao e N+1. Requisicoes de teste rodam em sequencia, entao o contador e global.
 */
public class QueryCounter implements StatementInspector {

    public static final String PROPERTY = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
            + "com.longobuccodev.app_adm_obras.support.QueryCounter";

    private static final List<String> SELECTS = new ArrayList<>();
    private static boolean recording;

    @Override
    public String inspect(String sql) {
        synchronized (SELECTS) {
            if (recording && sql.stripLeading().toLowerCase(Locale.ROOT).startsWith("select")) {
                SELECTS.add(sql);
            }
        }
        return sql;
    }

    /** Executa a acao e devolve os SELECT preparados durante ela. */
    public static Result count(Supplier<?> action) {
        synchronized (SELECTS) {
            SELECTS.clear();
            recording = true;
        }
        try {
            action.get();
        } finally {
            synchronized (SELECTS) {
                recording = false;
            }
        }
        synchronized (SELECTS) {
            return new Result(List.copyOf(SELECTS));
        }
    }

    public record Result(List<String> selects) {

        public int count() {
            return selects.size();
        }

        @Override
        public String toString() {
            return count() + " consultas:\n  " + String.join("\n  ", selects.stream()
                    .map(sql -> sql.replaceAll("\\s+", " "))
                    .toList());
        }
    }
}
