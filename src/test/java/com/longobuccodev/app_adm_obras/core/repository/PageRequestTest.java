package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.exception.InvalidPageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @Test
    @DisplayName("deve manter a pagina e o tamanho informados")
    void shouldKeepPageAndSize() {
        PageRequest request = PageRequest.of(2, 10);

        assertThat(request.page()).isEqualTo(2);
        assertThat(request.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("offset deve ser a multiplicacao entre pagina e tamanho")
    void offsetShouldBePageTimesSize() {
        assertThat(PageRequest.of(3, 25).offset()).isEqualTo(75);
    }

    @Test
    @DisplayName("firstPage deve usar a primeira pagina com o tamanho padrao")
    void firstPageShouldUseDefaults() {
        PageRequest request = PageRequest.firstPage();

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(PageRequest.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("ofSize deve usar a primeira pagina com o tamanho informado")
    void ofSizeShouldUseFirstPage() {
        assertThat(PageRequest.ofSize(5)).isEqualTo(new PageRequest(0, 5));
    }

    @Test
    @DisplayName("pagina negativa deve ser rejeitada")
    void shouldRejectNegativePage() {
        assertThatThrownBy(() -> PageRequest.of(-1, 10))
                .isInstanceOf(InvalidPageException.class)
                .hasMessageContaining("-1");
    }

    @Test
    @DisplayName("tamanho fora do intervalo de 1 a 100 deve ser rejeitado")
    void shouldRejectInvalidSize() {
        assertThatThrownBy(() -> PageRequest.of(0, 0))
                .isInstanceOf(InvalidPageException.class);
        assertThatThrownBy(() -> PageRequest.of(0, -5))
                .isInstanceOf(InvalidPageException.class);
        assertThatThrownBy(() -> PageRequest.of(0, PageRequest.MAX_SIZE + 1))
                .isInstanceOf(InvalidPageException.class);
    }

    @Test
    @DisplayName("os limites validos devem ser aceitos")
    void shouldAcceptBoundaries() {
        assertThat(PageRequest.of(0, 1).size()).isEqualTo(1);
        assertThat(PageRequest.of(0, PageRequest.MAX_SIZE).size()).isEqualTo(PageRequest.MAX_SIZE);
    }
}
