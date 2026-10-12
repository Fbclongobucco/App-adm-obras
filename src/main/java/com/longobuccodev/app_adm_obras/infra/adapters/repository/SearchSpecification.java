package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtro de busca das listagens: algum campo contem o termo, sem diferenciar maiusculas nem acentos.
 * Campos de relacao ("client.name") usam left join, para registros sem a relacao nao sumirem.
 * TRANSLATE existe com a mesma assinatura no H2 e no PostgreSQL, sem extensao de banco.
 */
final class SearchSpecification {

    private static final String ACCENTED = "áàâãäéèêëíìîïóòôõöúùûüçñ";
    private static final String PLAIN = "aaaaaeeeeiiiiooooouuuucn";

    private SearchSpecification() {
    }

    /**
     * @param textFields  campos comparados pelo texto normalizado
     * @param digitFields campos gravados so com digitos (CPF, CNPJ), comparados pelos digitos do termo
     */
    static <E> Specification<E> of(SearchTerm term, List<String> textFields, List<String> digitFields) {
        return (root, query, cb) -> {
            Map<String, From<?, ?>> joins = new HashMap<>();
            List<Predicate> any = new ArrayList<>();
            String text = "%" + term.text() + "%";
            for (String field : textFields) {
                Path<String> path = path(root, field, joins);
                any.add(cb.like(cb.function("translate", String.class, cb.lower(path),
                        cb.literal(ACCENTED), cb.literal(PLAIN)), text, SearchTerm.ESCAPE));
            }
            if (!term.digits().isEmpty()) {
                for (String field : digitFields) {
                    any.add(cb.like(path(root, field, joins), "%" + term.digits() + "%"));
                }
            }
            return cb.or(any.toArray(Predicate[]::new));
        };
    }

    private static Path<String> path(From<?, ?> root, String field, Map<String, From<?, ?>> joins) {
        String[] parts = field.split("\\.");
        From<?, ?> from = root;
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            key.append(parts[i]).append('.');
            From<?, ?> parent = from;
            String relation = parts[i];
            from = joins.computeIfAbsent(key.toString(), k -> parent.join(relation, JoinType.LEFT));
        }
        return from.get(parts[parts.length - 1]);
    }
}
