package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.EmployeeResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenter;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.employee;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.employeeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeUseCaseTest {

    private EmployeeRepository employeeRepository;
    private CostCenterRepository costCenterRepository;
    private EmployeeUseCase useCase;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        costCenterRepository = mock(CostCenterRepository.class);
        useCase = new EmployeeUseCase(employeeRepository, costCenterRepository);
    }

    @Test
    void shouldCreateEmployeeWithCostCenter() {
        CostCenter costCenter = costCenter();
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);
        when(employeeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponseDTO response = useCase.create(employeeRequest(costCenter.getId()));

        assertThat(response.costCenter().id()).isEqualTo(costCenter.getId());
        assertThat(response.costCenter().name()).isEqualTo("Obra Sao Paulo");
    }

    @Test
    void shouldRejectCreateWithUnknownCostCenter() {
        assertThatThrownBy(() -> useCase.create(employeeRequest(UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("cost_center.not_found");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldFindByCostCenterId() {
        CostCenter costCenter = costCenter();
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);
        when(employeeRepository.findByCostCenter(costCenter)).thenReturn(List.of(employee()));

        assertThat(useCase.findByCostCenterId(costCenter.getId())).hasSize(1);
    }

    @Test
    void shouldUpdateKeepingId() {
        Employee existing = employee();
        CostCenter costCenter = costCenter();
        when(employeeRepository.findById(existing.getId())).thenReturn(existing);
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);

        EmployeeResponseDTO response = useCase.update(existing.getId(), employeeRequest(costCenter.getId()));

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(employeeRepository).update(any(Employee.class));
    }
}
