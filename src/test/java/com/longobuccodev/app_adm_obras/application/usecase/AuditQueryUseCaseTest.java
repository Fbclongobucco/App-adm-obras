package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.audit.ChangeAuditResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.Action;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.EntityType;
import com.longobuccodev.app_adm_obras.core.exception.InvalidAuditFilterException;
import com.longobuccodev.app_adm_obras.core.repository.ChangeAuditRepository;
import com.longobuccodev.app_adm_obras.core.repository.LoginAuditRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuditQueryUseCaseTest {

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");
    private static final PageRequest PAGE = new PageRequest(0, 20);

    private ChangeAuditRepository changes;
    private LoginAuditRepository logins;
    private AuditQueryUseCase useCase;

    @BeforeEach
    void setUp() {
        changes = mock(ChangeAuditRepository.class);
        logins = mock(LoginAuditRepository.class);
        useCase = new AuditQueryUseCase(changes, logins);
    }

    @Test
    @DisplayName("Filtros validos devem chegar ao repositorio e o evento deve virar resposta")
    void shouldQueryWithParsedFilters() {
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ChangeAuditEntry entry = new ChangeAuditEntry(UUID.randomUUID(), EntityType.PROJECT, entityId, Action.UPDATE,
                T0, actorId, "Ana", "ana@email.com");
        when(changes.find(any(), any(), any(), any(), any(), any())).thenReturn(new Page<>(List.of(entry), 0, 20, 1, 1));

        PageResponseDTO<ChangeAuditResponseDTO> page = useCase.changes("PROJECT", entityId, actorId, T0,
                T0.plusSeconds(60), PAGE);

        verify(changes).find(EntityType.PROJECT, entityId, actorId, T0, T0.plusSeconds(60), PAGE);
        assertThat(page.content()).singleElement().satisfies(item -> {
            assertThat(item.entityType()).isEqualTo("PROJECT");
            assertThat(item.action()).isEqualTo("UPDATE");
            assertThat(item.actorName()).isEqualTo("Ana");
        });
    }

    @Test
    @DisplayName("Tipo vazio nao deve filtrar")
    void blankTypeShouldNotFilter() {
        when(changes.find(any(), any(), any(), any(), any(), any())).thenReturn(new Page<>(List.of(), 0, 20, 0, 0));

        useCase.changes(" ", null, null, null, null, PAGE);

        verify(changes).find(null, null, null, null, null, PAGE);
    }

    @Test
    @DisplayName("Tipo desconhecido deve ser rejeitado com audit.invalid.entityType, sem consultar")
    void unknownTypeShouldBeRejected() {
        assertThatThrownBy(() -> useCase.changes("obra", null, null, null, null, PAGE))
                .isInstanceOf(InvalidAuditFilterException.class)
                .extracting("errorCode").isEqualTo("audit.invalid.entityType");
        verifyNoInteractions(changes);
    }

    @Test
    @DisplayName("from depois de to deve ser rejeitado com audit.invalid.period nas duas consultas")
    void invertedPeriodShouldBeRejected() {
        assertThatThrownBy(() -> useCase.changes(null, null, null, T0.plusSeconds(1), T0, PAGE))
                .extracting("errorCode").isEqualTo("audit.invalid.period");
        assertThatThrownBy(() -> useCase.logins(null, T0.plusSeconds(1), T0, PAGE))
                .extracting("errorCode").isEqualTo("audit.invalid.period");
        verifyNoInteractions(changes, logins);
    }
}
