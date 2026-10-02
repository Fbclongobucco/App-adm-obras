package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.PageResponseDTO;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageMapperTest {

    @Test
    @DisplayName("toResponse deve converter o conteudo e os metadados")
    void toResponseShouldMapContentAndMetadata() {
        Page<String> page = new Page<>(List.of("a", "b"), 1, 2, 7, 4);

        PageResponseDTO<String> dto = PageMapper.toResponse(page, String::toUpperCase);

        assertThat(dto.content()).containsExactly("A", "B");
        assertThat(dto.page()).isEqualTo(1);
        assertThat(dto.size()).isEqualTo(2);
        assertThat(dto.totalElements()).isEqualTo(7);
        assertThat(dto.totalPages()).isEqualTo(4);
    }

    @Test
    @DisplayName("first e last devem refletir a posicao da pagina")
    void firstAndLastShouldReflectPosition() {
        assertThat(PageMapper.toResponse(new Page<>(List.of("a"), 0, 10, 30, 3)).first()).isTrue();
        assertThat(PageMapper.toResponse(new Page<>(List.of("a"), 0, 10, 30, 3)).last()).isFalse();
        assertThat(PageMapper.toResponse(new Page<>(List.of("a"), 1, 10, 30, 3)).first()).isFalse();
        assertThat(PageMapper.toResponse(new Page<>(List.of("a"), 1, 10, 30, 3)).last()).isFalse();
        assertThat(PageMapper.toResponse(new Page<>(List.of("a"), 2, 10, 30, 3)).first()).isFalse();
        assertThat(PageMapper.toResponse(new Page<>(List.of("a"), 2, 10, 30, 3)).last()).isTrue();
    }

    @Test
    @DisplayName("uma pagina unica deve ser a primeira e a ultima ao mesmo tempo")
    void singlePageShouldBeFirstAndLast() {
        PageResponseDTO<String> dto = PageMapper.toResponse(new Page<>(List.of("a"), 0, 10, 3, 1));

        assertThat(dto.first()).isTrue();
        assertThat(dto.last()).isTrue();
    }

    @Test
    @DisplayName("toResponse sem mapper deve preservar os elementos")
    void toResponseWithoutMapperShouldKeepElements() {
        PageResponseDTO<Integer> dto = PageMapper.toResponse(new Page<>(List.of(1, 2), 0, 10, 2, 1));

        assertThat(dto.content()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("toResponse deve retornar null quando a pagina for null")
    void toResponseShouldReturnNullForNullPage() {
        assertThat(PageMapper.toResponse(null)).isNull();
    }
}
