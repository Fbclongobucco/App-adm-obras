package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import org.springframework.data.domain.Pageable;

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

    static <E, D> Page<D> toDomain(org.springframework.data.domain.Page<E> source, Function<E, D> mapper) {
        return new Page<>(source.getContent().stream().map(mapper).toList(),
                source.getNumber(), source.getSize(), source.getTotalElements(), source.getTotalPages());
    }
}
