package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.core.exception.ConflictException;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/** Verificacoes de conflito feitas pelos casos de uso antes de gravar ou excluir. */
final class Conflicts {

    private Conflicts() {
    }

    /** Conflito se outro registro (id diferente de {@code selfId}) ja tem o valor. */
    static <T> void requireUnique(T found, Function<T, UUID> idOf, UUID selfId, Supplier<ConflictException> conflict) {
        if (found != null && !Objects.equals(idOf.apply(found), selfId)) {
            throw conflict.get();
        }
    }

    static void requireNone(Collection<?> dependents, Supplier<ConflictException> conflict) {
        if (dependents != null && !dependents.isEmpty()) {
            throw conflict.get();
        }
    }

    static void requireFalse(boolean linked, Supplier<ConflictException> conflict) {
        if (linked) {
            throw conflict.get();
        }
    }
}
