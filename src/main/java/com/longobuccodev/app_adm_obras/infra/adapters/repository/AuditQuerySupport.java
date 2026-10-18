package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/** Filtros e ordem comuns das consultas de auditoria. */
final class AuditQuerySupport {

    // Mais recente primeiro; id desempata eventos do mesmo instante entre paginas
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private AuditQuerySupport() {
    }

    static Pageable newestFirst(PageRequest request) {
        Pageable pageable = SpringPageSupport.toPageable(request);
        return org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                NEWEST_FIRST);
    }

    /** Igualdade quando o valor existe; sem valor, nao filtra. */
    static <E> Specification<E> equal(String attribute, Object value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(attribute), value);
    }

    /** {@code from} inclusivo, {@code to} exclusivo; extremos nulos ficam abertos. */
    static <E> Specification<E> between(Instant from, Instant to) {
        return (root, query, cb) -> {
            var occurredAt = root.<Instant>get("occurredAt");
            if (from != null && to != null) {
                return cb.and(cb.greaterThanOrEqualTo(occurredAt, from), cb.lessThan(occurredAt, to));
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(occurredAt, from);
            }
            return to == null ? null : cb.lessThan(occurredAt, to);
        };
    }
}
