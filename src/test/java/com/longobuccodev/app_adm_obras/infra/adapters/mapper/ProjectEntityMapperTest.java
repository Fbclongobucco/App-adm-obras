package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.infra.entities.AddressEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ClientEntity;
import com.longobuccodev.app_adm_obras.infra.entities.CostCenterEntity;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectEntityMapperTest {

    @Test
    @DisplayName("toDomain deve mapear os campos simples e os relacionamentos de ManyToOne")
    void toDomainShouldMapScalarFieldsAndManyToOneRelations() {
        ProjectEntity entity = projectEntity();

        Project project = ProjectEntityMapper.toDomain(entity, null, null);

        assertThat(project.getId()).isEqualTo(entity.getId());
        assertThat(project.getOs()).isEqualTo("OS-1234");
        assertThat(project.getDescription()).isEqualTo("Obra de reforma");
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(project.getEndDate()).isNull();
        assertThat(project.getIsCompleted()).isFalse();
        assertThat(project.getCostCenter().getName()).isEqualTo("Obra Sao Paulo");
        assertThat(project.getClient().getName()).isEqualTo("Construtora Alfa");
    }

    @Test
    @DisplayName("toDomain deve retornar null quando a entidade for null")
    void toDomainShouldReturnNullForNullEntity() {
        assertThat(ProjectEntityMapper.toDomain(null, null, null)).isNull();
    }

    @Test
    @DisplayName("toDomain deve ligar as refeicoes ao projeto e somar o total")
    void toDomainShouldLinkMealsAndCalculateTotal() {
        ProjectEntity entity = projectEntity();

        Project project = ProjectEntityMapper.toDomain(entity, lunch(), dinner());

        assertThat(project.getLunch().getRestaurantName()).isEqualTo("Restaurante do Ze");
        assertThat(project.getLunch().getProject()).isSameAs(project);
        assertThat(project.getDinner().getRestaurantName()).isEqualTo("Churrascaria do Boi");
        assertThat(project.getDinner().getProject()).isSameAs(project);
        assertThat(project.getTotalPrice()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("toDomainShallow nao deve carregar acomodacoes, funcionarios nem refeicoes")
    void toDomainShallowShouldNotLoadGraph() {
        Project project = ProjectEntityMapper.toDomainShallow(projectEntity());

        assertThat(project.getAccommodations()).isEmpty();
        assertThat(project.getEmployees()).isEmpty();
        assertThat(project.getLunch()).isNull();
        assertThat(project.getDinner()).isNull();
    }

    @Test
    @DisplayName("toEntity deve copiar os campos escalares e o id")
    void toEntityShouldMapScalarFields() {
        ProjectEntity entity = ProjectEntityMapper.toEntity(domainProject());

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getOs()).isEqualTo("OS-1234");
        assertThat(entity.getDescription()).isEqualTo("Obra de reforma");
        assertThat(entity.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("apply deve sobrescrever os campos escalares da entidade alvo")
    void applyShouldOverwriteScalarFields() {
        ProjectEntity target = ProjectEntity.create();
        target.setOs("OS-VELHO");

        ProjectEntityMapper.apply(domainProject(), target);

        assertThat(target.getOs()).isEqualTo("OS-1234");
    }

    @Test
    @DisplayName("os mappers nao devem ser afetados por dominio ou entidade nulos")
    void mappersShouldIgnoreNullArguments() {
        ProjectEntity entity = ProjectEntity.create();

        assertThat(ProjectEntityMapper.toEntity(null)).isNull();
        assertThat(ProjectEntityMapper.toDomainShallow(null)).isNull();

        ProjectEntityMapper.apply(null, entity);
        ProjectEntityMapper.apply(domainProject(), null);
        ProjectEntityMapper.apply(null, null);

        assertThat(entity.getOs()).isNull();
    }

    private Project domainProject() {
        return ProjectEntityMapper.toDomainShallow(projectEntity());
    }

    private ProjectEntity projectEntity() {
        CostCenterEntity costCenter = CostCenterEntity.create();
        costCenter.setId(UUID.randomUUID());
        costCenter.setName("Obra Sao Paulo");
        costCenter.setCnpj("11222333000181");

        ClientEntity client = ClientEntity.create();
        client.setId(UUID.randomUUID());
        client.setName("Construtora Alfa");
        client.setEmail("contato@alfa.com");
        client.setPhone("1133334444");
        client.setAddress(addressEntity());

        ProjectEntity entity = ProjectEntity.create();
        entity.setId(UUID.randomUUID());
        entity.setOs("OS-1234");
        entity.setDescription("Obra de reforma");
        entity.setCostCenter(costCenter);
        entity.setStartDate(LocalDate.of(2026, 1, 10));
        entity.setEndDate(null);
        entity.setClient(client);
        entity.setIsCompleted(false);
        entity.setTotalPrice(BigDecimal.ZERO);
        return entity;
    }

    private AddressEntity addressEntity() {
        AddressEntity address = AddressEntity.create();
        address.setId(UUID.randomUUID());
        address.setStreet("Rua das Flores");
        address.setNumber("120");
        address.setCity("Sao Paulo");
        address.setState("SP");
        address.setCountry("Brasil");
        address.setNeighborhood("Centro");
        address.setZipCode("01310100");
        return address;
    }

    private MealEntity lunch() {
        return meal(MealType.LUNCH, "Restaurante do Ze", "25.00", 10);
    }

    private MealEntity dinner() {
        return meal(MealType.DINNER, "Churrascaria do Boi", "45.00", 10);
    }

    private MealEntity meal(MealType mealType, String restaurantName, String price, int quantity) {
        MealEntity meal = MealEntity.create();
        meal.setId(UUID.randomUUID());
        meal.setRestaurantName(restaurantName);
        meal.setPrice(new BigDecimal(price));
        meal.setQuantity(quantity);
        meal.setTotalPrice(new BigDecimal(price).multiply(BigDecimal.valueOf(quantity)));
        meal.setIsBilled(false);
        meal.setMealType(mealType);
        meal.setDate(LocalDate.of(2026, 1, 15));
        return meal;
    }
}
