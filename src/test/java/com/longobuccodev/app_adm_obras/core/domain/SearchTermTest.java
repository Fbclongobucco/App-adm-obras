package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidPageException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchTermTest {

    @Test
    void nullEmptyOrBlankMeansNoSearch() {
        assertThat(SearchTerm.of(null)).isSameAs(SearchTerm.NONE);
        assertThat(SearchTerm.of("")).isSameAs(SearchTerm.NONE);
        assertThat(SearchTerm.of("   ")).isSameAs(SearchTerm.NONE);
        assertThat(SearchTerm.NONE.isNone()).isTrue();
    }

    @Test
    void shouldLowercaseRemoveAccentsAndTrim() {
        SearchTerm term = SearchTerm.of("  José   Antônio  ");

        assertThat(term.isNone()).isFalse();
        assertThat(term.text()).isEqualTo("jose antonio");
        assertThat(SearchTerm.of("FUNDIÇÃO").text()).isEqualTo("fundicao");
    }

    @Test
    void shouldEscapeLikeWildcards() {
        assertThat(SearchTerm.of("50%_off").text()).isEqualTo("50\\%\\_off");
        assertThat(SearchTerm.of("a\\b").text()).isEqualTo("a\\\\b");
    }

    @Test
    void shouldExposeOnlyDigits() {
        assertThat(SearchTerm.of("529.982").digits()).isEqualTo("529982");
        assertThat(SearchTerm.of("retrofit").digits()).isEmpty();
    }

    @Test
    void shouldAcceptUpTo100CharactersAfterTrim() {
        assertThat(SearchTerm.of(" " + "a".repeat(100) + " ").text()).hasSize(100);

        assertThatThrownBy(() -> SearchTerm.of("a".repeat(101)))
                .isInstanceOf(InvalidPageException.class)
                .extracting("errorCode").isEqualTo("page.invalid.query");
    }
}
