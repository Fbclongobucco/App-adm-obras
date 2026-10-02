package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.exception.InvalidEmployeeException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.address;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenter;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.employeeRequest;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeMapperTest {

    @Test
    void shouldRejectMissingCostCenter() {
        assertThatThrownBy(() -> EmployeeMapper.toDomain(employeeRequest(null), null))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.cost_center");
    }

    @Test
    void shouldKeepIdAddressAndProjectsWhenMappingOverExisting() {
        Project project = project();
        Employee existing = new Employee(null, "Joao da Silva", "joao@email.com", "529.982.247-25", null,
                address(), LocalDate.of(1990, 5, 20), costCenter(), Employee.Role.MONTADOR);
        existing.addProject(project);

        Employee updated = EmployeeMapper.toDomain(existing, employeeRequest(null), costCenter());

        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getAddress().getId()).isEqualTo(existing.getAddress().getId());
        assertThat(updated.getProjects()).containsExactly(project);
    }

    @Test
    void shouldMapDomainToResponseWithNestedObjects() {
        Project project = project();
        Employee employee = new Employee(null, "Joao da Silva", "joao@email.com", "529.982.247-25", null,
                address(), LocalDate.of(1990, 5, 20), costCenter(), Employee.Role.MONTADOR);
        employee.addProject(project);

        EmployeeResponseDTO response = EmployeeMapper.toResponse(employee);

        assertThat(response.costCenter().name()).isEqualTo("Obra Sao Paulo");
        assertThat(response.address().city()).isEqualTo("Sao Paulo");
        assertThat(response.projects()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(project.getId());
            assertThat(summary.client().name()).isEqualTo("Construtora Alfa");
        });
    }
}
