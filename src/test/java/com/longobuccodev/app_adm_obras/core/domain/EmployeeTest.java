package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Employee.Role;
import com.longobuccodev.app_adm_obras.core.exception.InvalidEmployeeException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeTest {

    private static Employee newEmployee() {
        return newEmployee(null);
    }

    private static Employee newEmployee(UUID id) {
        return newEmployee(id, null);
    }

    private static Employee newEmployee(UUID id, Set<Project> projects) {
        return new Employee(
                id,
                "Joao da Silva",
                "Joao.Silva@Email.com",
                "529.982.247-25",
                "(11) 98765-4321",
                null,
                LocalDate.of(1990, 5, 20),
                new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81"),
                projects,
                Role.MONTADOR
        );
    }

    private static Address newAddress() {
        return new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100");
    }

    private static Project newProject() {
        return new Project(null, "OS-1234", "Obra de reforma", new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81"),
                LocalDate.of(2026, 1, 10), null,
                new Client(null, "Construtora Alfa", "contato@alfa.com", "1133334444", newAddress(), null),
                null, null, false);
    }

    @Test
    void shouldNormalizeFieldsOnConstructor() {
        Employee employee = newEmployee();

        assertThat(employee.getId()).isNotNull();
        assertThat(employee.getName()).isEqualTo("Joao da Silva");
        assertThat(employee.getEmail()).isEqualTo("joao.silva@email.com");
        assertThat(employee.getCpf()).isEqualTo("52998224725");
        assertThat(employee.getPhone()).isEqualTo("11987654321");
        assertThat(employee.getProjects()).isEmpty();
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();

        assertThat(newEmployee(id).getId()).isEqualTo(id);
        assertThat(newEmployeeWithNullId().getId()).isNotNull();
    }

    private static Employee newEmployeeWithNullId() {
        Employee employee = newEmployee();
        employee.setId(null);
        return employee;
    }

    @Test
    void shouldCompareById() {
        UUID id = UUID.randomUUID();

        assertThat(newEmployee(id)).isEqualTo(newEmployee(id));
        assertThat(newEmployee(id)).isNotEqualTo(newEmployee());
    }

    @Test
    void shouldCopyProjectsOnConstructor() {
        Set<Project> projects = new HashSet<>();
        projects.add(newProject());

        Employee employee = newEmployee(null, projects);
        projects.add(newProject());

        assertThat(employee.getProjects()).hasSize(1);
    }

    @Test
    void shouldRejectBlankName() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setName("  "))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.name.blank");
    }

    @Test
    void shouldRejectShortName() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setName("Jo"))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.name.length");
    }

    @Test
    void shouldRejectLongName() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setName("a".repeat(101)))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.name.length");
    }

    @Test
    void shouldCollapseSpacesInName() {
        Employee employee = newEmployee();

        employee.setName("  Joao   da  Silva ");

        assertThat(employee.getName()).isEqualTo("Joao da Silva");
    }

    @Test
    void shouldRejectInvalidEmail() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setEmail("joao.silva"))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.email");
    }

    @Test
    void shouldRejectCpfWithWrongCheckDigits() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setCpf("529.982.247-26"))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.cpf");
    }

    @Test
    void shouldRejectCpfWithRepeatedDigits() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setCpf("111.111.111-11"))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.cpf");
    }

    @Test
    void shouldAcceptCpfWithoutMask() {
        Employee employee = newEmployee();

        assertThatNoException().isThrownBy(() -> employee.setCpf("11144477735"));
    }

    @Test
    void shouldRejectPhoneWithInvalidLength() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setPhone("1234"))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.phone");
    }

    @Test
    void shouldAcceptNullPhoneAndAddress() {
        Employee employee = newEmployee();
        Address address = newAddress();

        employee.setPhone(null);
        employee.setAddress(address);

        assertThat(employee.getPhone()).isNull();
        assertThat(employee.getAddress()).isSameAs(address);
    }

    @Test
    void shouldRejectFutureBirthDate() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setBirthDate(LocalDate.now().plusDays(1)))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.birth_date");
    }

    @Test
    void shouldRejectNullBirthDate() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setBirthDate(null))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.birth_date");
    }

    @Test
    void shouldRejectMissingRole() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setRole(null))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.role");
    }

    @Test
    void shouldRejectMissingCostCenter() {
        Employee employee = newEmployee();

        assertThatThrownBy(() -> employee.setCostCenter(null))
                .isInstanceOf(InvalidEmployeeException.class)
                .extracting("errorCode").isEqualTo("employee.invalid.cost_center");
    }

    @Test
    void shouldValidateOnConstructor() {
        assertThatThrownBy(() -> new Employee(
                null, null, "joao@email.com", "52998224725", null, null,
                LocalDate.of(1990, 5, 20), new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81"), null, Role.MONTADOR
        )).isInstanceOf(InvalidEmployeeException.class);
    }
}