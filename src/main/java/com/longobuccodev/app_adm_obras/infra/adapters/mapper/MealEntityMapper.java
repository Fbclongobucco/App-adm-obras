package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;

public final class MealEntityMapper {

    private MealEntityMapper() {
    }

    public static Meal toDomain(MealEntity entity) {
        return toDomain(entity, true);
    }

    public static Meal toDomain(MealEntity entity, boolean withProject) {
        if (entity == null) {
            return null;
        }
        return new Meal(entity.getId(), entity.getRestaurantName(), entity.getPrice(), entity.getIsBilled(),
                AddressEntityMapper.toDomain(entity.getAddress()),
                withProject ? ProjectEntityMapper.toDomainShallow(entity.getProject()) : null,
                entity.getMealType(), entity.getQuantity(), entity.getDate());
    }

    public static MealEntity toEntity(Meal domain) {
        if (domain == null) {
            return null;
        }
        MealEntity entity = MealEntity.create();
        entity.setId(domain.getId());
        entity.setAddress(AddressEntityMapper.toEntity(domain.getAddress()));
        apply(domain, entity);
        return entity;
    }

    public static void apply(Meal domain, MealEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setRestaurantName(domain.getRestaurantName());
        entity.setPrice(domain.getPrice());
        entity.setIsBilled(domain.getBilled());
        entity.setMealType(domain.getMealType());
        entity.setQuantity(domain.getQuantity());
        entity.setDate(domain.getDate());
        entity.setTotalPrice(domain.getTotalPrice());
        if (domain.getAddress() == null) {
            entity.setAddress(null);
        } else if (entity.getAddress() == null) {
            entity.setAddress(AddressEntityMapper.toEntity(domain.getAddress()));
        } else {
            AddressEntityMapper.apply(domain.getAddress(), entity.getAddress());
        }
    }
}
