package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

final class EntityLookup {

    private EntityLookup() {
    }

    static <T> T require(UUID id, Function<UUID, T> finder, Function<UUID, ResourceNotFoundException> notFound) {
        T entity = finder.apply(id);
        if (entity == null) {
            throw notFound.apply(id);
        }
        return entity;
    }

    static <T> T optional(UUID id, Function<UUID, T> finder, Function<UUID, ResourceNotFoundException> notFound) {
        return id == null ? null : require(id, finder, notFound);
    }

    /**
     * Busca todos os ids em uma leitura e devolve na ordem pedida. O primeiro id inexistente, na ordem
     * pedida, responde como no {@link #require}.
     */
    static <T> Set<T> all(Set<UUID> ids, Function<Collection<UUID>, List<T>> finder, Function<T, UUID> idOf,
                          Function<UUID, ResourceNotFoundException> notFound) {
        Set<T> entities = new LinkedHashSet<>();
        if (ids == null || ids.isEmpty()) {
            return entities;
        }
        Map<UUID, T> found = new HashMap<>();
        finder.apply(ids).forEach(entity -> found.put(idOf.apply(entity), entity));
        for (UUID id : ids) {
            T entity = found.get(id);
            if (entity == null) {
                throw notFound.apply(id);
            }
            entities.add(entity);
        }
        return entities;
    }
}
