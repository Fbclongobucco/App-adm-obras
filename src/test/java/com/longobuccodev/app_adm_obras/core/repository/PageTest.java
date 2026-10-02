package com.longobuccodev.app_adm_obras.core.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

    @Test
    @DisplayName("deve expor conteudo e metadados")
    void shouldExposeContentAndMetadata() {
        Page<String> page = new Page<>(List.of("a", "b"), 1, 2, 5, 3);

        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("conteudo nulo deve virar lista vazia")
    void nullContentShouldBecomeEmptyList() {
        assertThat(new Page<String>(null, 0, 10, 0, 0).content()).isEmpty();
    }

    @Test
    @DisplayName("isEmpty deve refletir o conteudo")
    void isEmptyShouldReflectContent() {
        assertThat(new Page<>(List.of(), 0, 10, 0, 0).isEmpty()).isTrue();
        assertThat(new Page<>(List.of("a"), 0, 10, 1, 1).isEmpty()).isFalse();
    }

    @Test
    @DisplayName("hasNext e hasPrevious devem refletir a posicao atual")
    void navigationFlagsShouldReflectCurrentPosition() {
        assertThat(new Page<>(List.of("a"), 0, 10, 25, 3).hasPrevious()).isFalse();
        assertThat(new Page<>(List.of("a"), 0, 10, 25, 3).hasNext()).isTrue();
        assertThat(new Page<>(List.of("a"), 1, 10, 25, 3).hasPrevious()).isTrue();
        assertThat(new Page<>(List.of("a"), 1, 10, 25, 3).hasNext()).isTrue();
        assertThat(new Page<>(List.of("a"), 2, 10, 25, 3).hasPrevious()).isTrue();
        assertThat(new Page<>(List.of("a"), 2, 10, 25, 3).hasNext()).isFalse();
    }

    @Test
    @DisplayName("o conteudo deve ser imutavel")
    void contentShouldBeImmutable() {
        Page<String> page = new Page<>(new java.util.ArrayList<>(List.of("a")), 0, 10, 1, 1);

        assertThatThrownBy(() -> page.content().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
