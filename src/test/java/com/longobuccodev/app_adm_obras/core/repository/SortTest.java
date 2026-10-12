package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.exception.InvalidPageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortTest {

    private static final Set<String> FIELDS = Set.of("updatedAt", "os");

    @Test
    @DisplayName("campo,direcao deve ser lido com a direcao informada")
    void shouldParseFieldAndDirection() {
        assertThat(Sort.parse("updatedAt,desc", FIELDS)).isEqualTo(new Sort("updatedAt", Sort.Direction.DESC));
        assertThat(Sort.parse("os,ASC", FIELDS)).isEqualTo(new Sort("os", Sort.Direction.ASC));
    }

    @Test
    @DisplayName("sem direcao deve usar asc")
    void directionShouldDefaultToAscending() {
        assertThat(Sort.parse("os", FIELDS)).isEqualTo(new Sort("os", Sort.Direction.ASC));
    }

    @Test
    @DisplayName("texto vazio ou nulo significa sem ordenacao")
    void blankShouldMeanNoSort() {
        assertThat(Sort.parse(null, FIELDS)).isNull();
        assertThat(Sort.parse("  ", FIELDS)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"totalPrice,desc", "updatedAt,up", "updatedAt,desc,os", ",desc"})
    @DisplayName("campo ou direcao fora do permitido deve ser rejeitado com page.invalid.sort")
    void shouldRejectUnsupportedSort(String sort) {
        assertThatThrownBy(() -> Sort.parse(sort, FIELDS))
                .isInstanceOf(InvalidPageException.class)
                .hasFieldOrPropertyWithValue("errorCode", "page.invalid.sort");
    }

    @Test
    @DisplayName("PageRequest sem ordenacao continua valido e withSort preserva pagina e tamanho")
    void pageRequestShouldCarrySort() {
        PageRequest request = new PageRequest(2, 10);

        assertThat(request.sort()).isNull();
        assertThat(request.withSort(new Sort("os", Sort.Direction.DESC)))
                .isEqualTo(new PageRequest(2, 10, new Sort("os", Sort.Direction.DESC)));
    }
}
