package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Carga de vinculos por lista de ids em uma consulta, no lugar de um findById por item. */
final class BatchLookup {

    private BatchLookup() {
    }

    /** Busca os ids em uma consulta e devolve na ordem pedida; id inexistente e ignorado. */
    static <E> List<E> inOrder(List<UUID> ids, Function<List<UUID>, List<E>> finder, Function<E, UUID> idOf) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, E> found = finder.apply(ids).stream().collect(Collectors.toMap(idOf, Function.identity()));
        return ids.stream().map(found::get).filter(Objects::nonNull).toList();
    }
}
