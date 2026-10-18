package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class SpringPageSupport {

    private SpringPageSupport() {
    }

    static Pageable toPageable(PageRequest request) {
        if (request == null) {
            return org.springframework.data.domain.PageRequest.of(0, PageRequest.DEFAULT_SIZE);
        }
        return org.springframework.data.domain.PageRequest.of(request.page(), request.size());
    }

    /**
     * Traduz a ordenacao do dominio para a propriedade da entidade, com desempate fixo por id
     * para que paginas consecutivas nao repitam nem pulem registros.
     */
    static Pageable toPageable(PageRequest request, Map<String, String> entityProperties) {
        if (request == null || request.sort() == null) {
            return toPageable(request);
        }
        String property = entityProperties.get(request.sort().field());
        if (property == null) {
            throw new IllegalArgumentException("Sort field without entity mapping: " + request.sort().field());
        }
        Sort.Direction direction = Sort.Direction.valueOf(request.sort().direction().name());
        return org.springframework.data.domain.PageRequest.of(request.page(), request.size(),
                Sort.by(direction, property).and(Sort.by(direction, "id")));
    }

    static <E, D> Page<D> toDomain(org.springframework.data.domain.Page<E> source, Function<E, D> mapper) {
        return new Page<>(source.getContent().stream().map(mapper).toList(),
                source.getNumber(), source.getSize(), source.getTotalElements(), source.getTotalPages());
    }

    /** Mapeia a pagina inteira de uma vez, para o adaptador carregar o que falta em lote. */
    static <E, D> Page<D> toDomainAll(org.springframework.data.domain.Page<E> source,
                                      Function<List<E>, List<D>> mapper) {
        return new Page<>(mapper.apply(source.getContent()),
                source.getNumber(), source.getSize(), source.getTotalElements(), source.getTotalPages());
    }
}
