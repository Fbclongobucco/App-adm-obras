package com.longobuccodev.app_adm_obras.application;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.client.ClientRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectRequestDTO;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Address;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Employee.Role;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public final class ApplicationFixtures {

    private ApplicationFixtures() {
    }

    public static AddressRequestDTO addressRequest() {
        return new AddressRequestDTO("Rua das Flores", "120", "Sao Paulo", "sp", "Brasil", "Centro", "01310-100");
    }

    public static CostCenterRequestDTO costCenterRequest() {
        return new CostCenterRequestDTO("Obra Sao Paulo", "11.222.333/0001-81");
    }

    public static ClientRequestDTO clientRequest() {
        return new ClientRequestDTO("Construtora Alfa", "contato@alfa.com", "1133334444", addressRequest());
    }

    public static EmployeeRequestDTO employeeRequest(UUID costCenterId) {
        return new EmployeeRequestDTO("Joao da Silva", "joao@email.com", "529.982.247-25", null, addressRequest(),
                LocalDate.of(1990, 5, 20), costCenterId, Role.MONTADOR);
    }

    /** Almoco 10 x 25.00 e jantar 8 x 22.00; o tipo decide quais campos valem. */
    public static MealRequestDTO mealRequest(UUID restaurantId, MealType mealType) {
        return new MealRequestDTO(restaurantId, mealType, false, 10, new BigDecimal("25.00"), 8,
                new BigDecimal("22.00"));
    }

    public static ProjectRequestDTO projectRequest(UUID costCenterId, UUID clientId, Set<UUID> accommodationIds,
                                                   Set<UUID> employeeIds) {
        return new ProjectRequestDTO("os-1234", "Obra de reforma", costCenterId, LocalDate.of(2026, 1, 10), null,
                clientId, accommodationIds, employeeIds, false);
    }

    public static Address address() {
        return new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100");
    }

    public static CostCenter costCenter() {
        return new CostCenter(null, "Obra Sao Paulo", "11.222.333/0001-81");
    }

    public static Client client() {
        return new Client(null, "Construtora Alfa", "contato@alfa.com", "1133334444", address());
    }

    public static Employee employee() {
        return new Employee(null, "Joao da Silva", "joao@email.com", "529.982.247-25", null, address(),
                LocalDate.of(1990, 5, 20), costCenter(), Role.MONTADOR);
    }

    public static Restaurant restaurant(Project project) {
        return restaurant(project, null);
    }

    public static Restaurant restaurant(Project project, BigDecimal deliveryFee) {
        return restaurant(project, deliveryFee, null);
    }

    public static Restaurant restaurant(Project project, BigDecimal deliveryFee, BigDecimal sodaPrice) {
        return new Restaurant(null, "Restaurante do Ze", "1133335555", address(), project, deliveryFee, sodaPrice);
    }

    public static Meal meal(MealType mealType) {
        return meal(mealType, project());
    }

    public static Meal meal(MealType mealType, Project project) {
        return new Meal(null, restaurant(project), mealType, false, 10, new BigDecimal("25.00"), 8,
                new BigDecimal("22.00"));
    }

    public static Accommodation accommodation() {
        return new Accommodation(null, "Maria Souza", "11988887777", address(), 8, false, null,
                LocalDate.of(2026, 10, 12), new BigDecimal("1000.00"));
    }

    public static Project project() {
        return new Project(null, "OS-1234", "Obra de reforma", costCenter(), LocalDate.of(2026, 1, 10), null,
                client(), false);
    }
}
