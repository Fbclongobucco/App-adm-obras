package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Employee.Role;
import com.longobuccodev.app_adm_obras.core.exception.InvalidProjectException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelationshipSyncTest {

    private static Address newAddress() {
        return new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100");
    }

    private static Client newClient(String name) {
        return new Client(null, name, "contato@alfa.com", "1133334444", newAddress());
    }

    private static Project newProject(Client client) {
        return new Project(null, "OS-1234", "Obra de reforma",
                new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81"), LocalDate.of(2026, 1, 10), null,
                client, false);
    }

    private static Accommodation newAccommodation(Project project, String totalPrice) {
        return new Accommodation(null, "Maria Souza", "11988887777", newAddress(), 8, 30, false, project,
                new BigDecimal(totalPrice));
    }

    private static Employee newEmployee() {
        return new Employee(null, "Joao da Silva", "joao@email.com", "529.982.247-25", null, null,
                LocalDate.of(1990, 5, 20), new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81"),
                Role.MONTADOR);
    }

    @Test
    void shouldLinkProjectToClientOnConstructor() {
        Client client = newClient("Construtora Alfa");

        Project project = newProject(client);

        assertThat(client.getProjects()).containsExactly(project);
    }

    @Test
    void shouldMoveProjectWhenClientChanges() {
        Client first = newClient("Construtora Alfa");
        Client second = newClient("Construtora Beta");
        Project project = newProject(first);

        project.setClient(second);

        assertThat(first.getProjects()).isEmpty();
        assertThat(second.getProjects()).containsExactly(project);
    }

    @Test
    void shouldMoveProjectWhenAddedToAnotherClient() {
        Client first = newClient("Construtora Alfa");
        Client second = newClient("Construtora Beta");
        Project project = newProject(first);

        second.addProject(project);

        assertThat(project.getClient()).isSameAs(second);
        assertThat(first.getProjects()).isEmpty();
    }

    @Test
    void shouldLinkAccommodationToProjectOnConstructor() {
        Project project = newProject(newClient("Construtora Alfa"));

        Accommodation accommodation = newAccommodation(project, "1000.00");

        assertThat(project.getAccommodations()).containsExactly(accommodation);
        assertThat(project.getTotalPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldLinkAccommodationWhenAddedToProject() {
        Project project = newProject(newClient("Construtora Alfa"));
        Accommodation accommodation = newAccommodation(null, "1000.00");

        project.addAccommodation(accommodation);

        assertThat(accommodation.getProject()).isSameAs(project);
        assertThat(project.getTotalPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldMoveAccommodationBetweenProjects() {
        Project first = newProject(newClient("Construtora Alfa"));
        Project second = newProject(newClient("Construtora Beta"));
        Accommodation accommodation = newAccommodation(first, "1000.00");

        accommodation.setProject(second);

        assertThat(first.getAccommodations()).isEmpty();
        assertThat(first.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(second.getAccommodations()).containsExactly(accommodation);
        assertThat(second.getTotalPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldUnlinkAccommodationWhenRemovedFromProject() {
        Project project = newProject(newClient("Construtora Alfa"));
        Accommodation accommodation = newAccommodation(project, "1000.00");

        project.removeAccommodation(accommodation);

        assertThat(accommodation.getProject()).isNull();
        assertThat(project.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRefreshProjectTotalWhenAccommodationPriceChanges() {
        Project project = newProject(newClient("Construtora Alfa"));
        Accommodation accommodation = newAccommodation(project, "1000.00");

        accommodation.setTotalPrice(new BigDecimal("1200.00"));

        assertThat(project.getTotalPrice()).isEqualByComparingTo("1200.00");
    }

    @Test
    void shouldLinkEmployeeAndProjectInBothDirections() {
        Project project = newProject(newClient("Construtora Alfa"));
        Employee employee = newEmployee();

        project.addEmployee(employee);

        assertThat(employee.getProjects()).containsExactly(project);

        employee.removeProject(project);

        assertThat(project.getEmployees()).isEmpty();
    }

    @Test
    void shouldLinkEmployeeProjectsWhenProjectIsAdded() {
        Project project = newProject(newClient("Construtora Alfa"));
        Employee employee = newEmployee();

        employee.addProject(project);

        assertThat(project.getEmployees()).containsExactly(employee);
    }

    @Test
    void shouldExposeReadOnlyCollections() {
        Project project = newProject(newClient("Construtora Alfa"));

        assertThatThrownBy(() -> project.getAccommodations().add(newAccommodation(null, "10.00")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> project.getEmployees().add(newEmployee()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> project.getClient().getProjects().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectNullAccommodation() {
        Project project = newProject(newClient("Construtora Alfa"));

        assertThatThrownBy(() -> project.addAccommodation(null))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.accommodation");
    }
}
