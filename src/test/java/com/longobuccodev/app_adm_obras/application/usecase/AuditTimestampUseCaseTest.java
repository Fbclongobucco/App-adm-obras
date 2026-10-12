package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.client.ClientRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.client.ClientResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Address;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.exception.CoreDomainException;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditTimestampUseCaseTest {

    private static final Instant CREATED = Instant.parse("2026-01-10T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String CNPJ = "11222333000181";

    @Test
    @DisplayName("criacao deve gravar criacao e alteracao com o instante do relogio")
    void createShouldStampWithClock() {
        CostCenterRepository repository = mock(CostCenterRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CostCenterResponseDTO response = costCenterUseCase(repository)
                .create(new CostCenterRequestDTO("Centro Norte", CNPJ));

        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("alteracao deve preservar a criacao e avancar a alteracao (mapper que cria objeto novo)")
    void updateShouldKeepCreatedAtWhenMapperBuildsNewObject() {
        CostCenterRepository repository = mock(CostCenterRepository.class);
        CostCenter existing = new CostCenter(UUID.randomUUID(), "Centro Norte", CNPJ);
        existing.markCreated(CREATED);
        when(repository.findById(existing.getId())).thenReturn(existing);

        CostCenterResponseDTO response = costCenterUseCase(repository)
                .update(existing.getId(), new CostCenterRequestDTO("Centro Sul", CNPJ));

        ArgumentCaptor<CostCenter> saved = ArgumentCaptor.forClass(CostCenter.class);
        verify(repository).update(saved.capture());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(CREATED);
        assertThat(saved.getValue().getUpdatedAt()).isEqualTo(NOW);
        assertThat(response.createdAt()).isEqualTo(CREATED);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("alteracao deve preservar a criacao e avancar a alteracao (mapper que altera o existente)")
    void updateShouldKeepCreatedAtWhenMapperMutatesExisting() {
        ClientRepository repository = mock(ClientRepository.class);
        Client existing = new Client(UUID.randomUUID(), "Cliente Alfa", "alfa@exemplo.test", "1933334444",
                new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000"));
        existing.markCreated(CREATED);
        when(repository.findById(existing.getId())).thenReturn(existing);

        ClientResponseDTO response = clientUseCase(repository).update(existing.getId(),
                new ClientRequestDTO("Cliente Beta", "beta@exemplo.test", "1933334444",
                        new AddressRequestDTO("Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000")));

        assertThat(response.createdAt()).isEqualTo(CREATED);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("alteracao rejeitada nao deve gravar nem mudar a data de alteracao")
    void rejectedUpdateShouldKeepTimestamps() {
        CostCenterRepository repository = mock(CostCenterRepository.class);
        CostCenter existing = new CostCenter(UUID.randomUUID(), "Centro Norte", CNPJ);
        existing.markCreated(CREATED);
        when(repository.findById(existing.getId())).thenReturn(existing);

        assertThatThrownBy(() -> costCenterUseCase(repository)
                .update(existing.getId(), new CostCenterRequestDTO("Centro Sul", "123")))
                .isInstanceOf(CoreDomainException.class);

        verify(repository, never()).update(any());
        assertThat(existing.getUpdatedAt()).isEqualTo(CREATED);
    }

    private static CostCenterUseCase costCenterUseCase(CostCenterRepository repository) {
        return new CostCenterUseCase(repository, mock(EmployeeRepository.class), mock(ProjectRepository.class), CLOCK);
    }

    private static ClientUseCase clientUseCase(ClientRepository repository) {
        return new ClientUseCase(repository, mock(ProjectRepository.class), CLOCK);
    }
}
