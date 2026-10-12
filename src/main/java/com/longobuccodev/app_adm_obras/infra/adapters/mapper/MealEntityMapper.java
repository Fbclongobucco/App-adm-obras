package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import com.longobuccodev.app_adm_obras.infra.entities.RestaurantEntity;

public final class MealEntityMapper {

    private MealEntityMapper() {
    }

    /** A obra da refeicao e a do restaurante, lida rasa. */
    public static Meal toDomain(MealEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuditMapping.restore(new Meal(entity.getId(), RestaurantEntityMapper.toDomain(entity.getRestaurant()),
                entity.getPrice(), entity.getIsBilled(), entity.getMealType(), entity.getQuantity(),
                entity.getDate()), entity);
    }

    /** @param restaurant referencia gerenciada ao restaurante; a obra da refeicao e copiada dele */
    public static MealEntity toEntity(Meal domain, RestaurantEntity restaurant) {
        if (domain == null) {
            return null;
        }
        MealEntity entity = MealEntity.create();
        entity.setId(domain.getId());
        apply(domain, entity, restaurant);
        return entity;
    }

    public static void apply(Meal domain, MealEntity entity, RestaurantEntity restaurant) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setRestaurant(restaurant);
        entity.setProject(restaurant == null ? null : restaurant.getProject());
        entity.setPrice(domain.getPrice());
        entity.setIsBilled(domain.getBilled());
        entity.setMealType(domain.getMealType());
        entity.setQuantity(domain.getQuantity());
        entity.setDate(domain.getDate());
        entity.setTotalPrice(domain.getTotalPrice());
        AuditMapping.copy(domain, entity);
    }
}
