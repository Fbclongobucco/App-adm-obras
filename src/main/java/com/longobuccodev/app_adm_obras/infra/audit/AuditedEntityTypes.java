package com.longobuccodev.app_adm_obras.infra.audit;

import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.EntityType;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationEntity;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationPeriodEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ClientEntity;
import com.longobuccodev.app_adm_obras.infra.entities.CostCenterEntity;
import com.longobuccodev.app_adm_obras.infra.entities.EmployeeEntity;
import com.longobuccodev.app_adm_obras.infra.entities.MealAdjustmentEntity;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.entities.RestaurantEntity;
import com.longobuccodev.app_adm_obras.infra.entities.UserEntity;

import java.util.Map;
import java.util.Optional;

/**
 * Entidades auditadas e o tipo de registro de cada uma. Lista fechada: endereco (parte do dono), refresh
 * token e as proprias tabelas de auditoria ficam fora.
 */
final class AuditedEntityTypes {

    private static final Map<Class<?>, EntityType> TYPES = Map.of(
            ProjectEntity.class, EntityType.PROJECT,
            EmployeeEntity.class, EntityType.EMPLOYEE,
            CostCenterEntity.class, EntityType.COST_CENTER,
            ClientEntity.class, EntityType.CLIENT,
            AccommodationEntity.class, EntityType.ACCOMMODATION,
            AccommodationPeriodEntity.class, EntityType.ACCOMMODATION_PERIOD,
            MealEntity.class, EntityType.MEAL,
            MealAdjustmentEntity.class, EntityType.MEAL_ADJUSTMENT,
            RestaurantEntity.class, EntityType.RESTAURANT,
            UserEntity.class, EntityType.USER);

    private AuditedEntityTypes() {
    }

    /** Vazio para entidade nao auditada. Os eventos do Hibernate trazem a instancia real, nao um proxy. */
    static Optional<EntityType> of(Object entity) {
        return Optional.ofNullable(TYPES.get(entity.getClass()));
    }
}
