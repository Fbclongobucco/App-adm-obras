package com.longobuccodev.app_adm_obras.application;

import com.longobuccodev.app_adm_obras.application.dto.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.ClientRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.CostCenterRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.EmployeeRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.ProjectRequestDTO;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Address;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Employee.Role;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;

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

    public static MealRequestDTO mealRequest(UUID projectId, MealType mealType) {
        return new MealRequestDTO("Restaurante do Ze", new BigDecimal("25.00"), false, null, projectId, mealType,
                10, LocalDate.of(2026, 1, 15));
    }

    public static ProjectRequestDTO projectRequest(UUID costCenterId, UUID clientId, Set<UUID> accommodationIds,
                                                   Set<UUID> employeeIds, UUID lunchId, UUID dinnerId) {
        return new ProjectRequestDTO("os-1234", "Obra de reforma", costCenterId, LocalDate.of(2026, 1, 10), null,
                clientId, accommodationIds, employeeIds, lunchId, dinnerId, false);
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

    public static Meal meal(MealType mealType) {
        return new Meal(null, "Restaurante do Ze", new BigDecimal("25.00"), false, null, null, mealType, 10,
                LocalDate.of(2026, 1, 15));
    }

    public static Accommodation accommodation() {
        return new Accommodation(null, "Maria Souza", "11988887777", address(), 8, 30, false, null,
                new BigDecimal("1000.00"));
    }

    public static Project project() {
        return new Project(null, "OS-1234", "Obra de reforma", costCenter(), LocalDate.of(2026, 1, 10), null,
                client(), false);
    }
}
