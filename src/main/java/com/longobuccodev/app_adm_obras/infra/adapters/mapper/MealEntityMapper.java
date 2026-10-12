package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;
import com.longobuccodev.app_adm_obras.infra.entities.MealAdjustmentEntity;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import com.longobuccodev.app_adm_obras.infra.entities.RestaurantEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MealEntityMapper {

    private MealEntityMapper() {
    }

    /** A obra da refeicao e a do restaurante, lida rasa. Os ajustes por data vem junto. */
    public static Meal toDomain(MealEntity entity) {
        if (entity == null) {
            return null;
        }
        Meal meal = AuditMapping.restore(new Meal(entity.getId(),
                RestaurantEntityMapper.toDomain(entity.getRestaurant()), entity.getMealType(), entity.getIsBilled(),
                entity.getLunchQuantity(), entity.getLunchPrice(), entity.getDinnerQuantity(),
                entity.getDinnerPrice()), entity);
        if (entity.getAdjustments() != null) {
            entity.getAdjustments().forEach(adjustment -> meal.restoreAdjustment(toDomain(adjustment)));
        }
        return meal;
    }

    private static MealAdjustment toDomain(MealAdjustmentEntity entity) {
        return AuditMapping.restore(new MealAdjustment(entity.getId(), entity.getDate(), entity.getType(),
                entity.getLunchQuantity(), entity.getDinnerQuantity(), entity.getNote()), entity);
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
        entity.setIsBilled(domain.getBilled());
        entity.setMealType(domain.getMealType());
        entity.setLunchQuantity(domain.getLunchQuantity());
        entity.setLunchPrice(domain.getLunchPrice());
        entity.setDinnerQuantity(domain.getDinnerQuantity());
        entity.setDinnerPrice(domain.getDinnerPrice());
        // Coberturas para a unicidade no banco: TRUE ou NULL, nunca FALSE
        entity.setLunchSlot(domain.getMealType().servesLunch() ? Boolean.TRUE : null);
        entity.setDinnerSlot(domain.getMealType().servesDinner() ? Boolean.TRUE : null);
        AuditMapping.copy(domain, entity);
        applyAdjustments(domain, entity);
    }

    /** Sincroniza os ajustes por id: inclui os novos, remove os ausentes e atualiza os existentes. */
    private static void applyAdjustments(Meal domain, MealEntity entity) {
        Set<UUID> kept = domain.getAdjustments().stream().map(MealAdjustment::getId).collect(Collectors.toSet());
        entity.getAdjustments().removeIf(adjustment -> !kept.contains(adjustment.getId()));
        Map<UUID, MealAdjustmentEntity> current = entity.getAdjustments().stream()
                .collect(Collectors.toMap(MealAdjustmentEntity::getId, Function.identity()));
        for (MealAdjustment adjustment : domain.getAdjustments()) {
            MealAdjustmentEntity target = current.get(adjustment.getId());
            if (target == null) {
                target = MealAdjustmentEntity.create();
                target.setId(adjustment.getId());
                target.setMeal(entity);
                entity.getAdjustments().add(target);
            }
            target.setDate(adjustment.getDate());
            target.setType(adjustment.getType());
            target.setLunchQuantity(adjustment.getLunchQuantity());
            target.setDinnerQuantity(adjustment.getDinnerQuantity());
            target.setNote(adjustment.getNote());
            AuditMapping.copy(adjustment, target);
        }
    }
}
