package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;

import java.util.LinkedHashSet;
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

    static <T> Set<T> all(Set<UUID> ids, Function<UUID, T> finder,
                          Function<UUID, ResourceNotFoundException> notFound) {
        Set<T> entities = new LinkedHashSet<>();
        if (ids == null) {
            return entities;
        }
        for (UUID id : ids) {
            entities.add(require(id, finder, notFound));
        }
        return entities;
    }
}
