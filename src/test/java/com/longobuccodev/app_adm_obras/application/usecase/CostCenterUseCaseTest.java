package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenter;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenterRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CostCenterUseCaseTest {

    static final Instant CLOCK_NOW = Instant.parse("2026-01-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(CLOCK_NOW, ZoneOffset.UTC);

    private CostCenterRepository costCenterRepository;
    private CostCenterUseCase useCase;

    @BeforeEach
    void setUp() {
        costCenterRepository = mock(CostCenterRepository.class);
        useCase = new CostCenterUseCase(costCenterRepository, mock(EmployeeRepository.class),
                mock(ProjectRepository.class), CLOCK);
    }

    @Test
    void shouldCreateCostCenter() {
        when(costCenterRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CostCenterResponseDTO response = useCase.create(costCenterRequest());

        assertThat(response.cnpj()).isEqualTo("11222333000181");
    }

    @Test
    void shouldUpdateKeepingId() {
        CostCenter existing = costCenter();
        when(costCenterRepository.findById(existing.getId())).thenReturn(existing);

        CostCenterResponseDTO response = useCase.update(existing.getId(), costCenterRequest());

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(costCenterRepository).update(any(CostCenter.class));
    }

    @Test
    void shouldRejectFindByUnknownId() {
        assertThatThrownBy(() -> useCase.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("cost_center.not_found");
    }
}
