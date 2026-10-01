package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    private CostCenterRepository costCenterRepository;
    private CostCenterUseCase useCase;

    @BeforeEach
    void setUp() {
        costCenterRepository = mock(CostCenterRepository.class);
        useCase = new CostCenterUseCase(costCenterRepository);
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
