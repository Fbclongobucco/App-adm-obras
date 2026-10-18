package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Restaurant;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.entities.RestaurantEntity;

public final class RestaurantEntityMapper {

    private RestaurantEntityMapper() {
    }

    /** A obra vem rasa (sem refeicoes nem restaurantes), para nao recursar na leitura da obra. */
    public static Restaurant toDomain(RestaurantEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuditMapping.restore(new Restaurant(entity.getId(), entity.getName(), entity.getPhone(),
                AddressEntityMapper.toDomain(entity.getAddress()),
                ProjectEntityMapper.toDomainShallow(entity.getProject()), entity.getLunchDeliveryFee(),
                entity.getDinnerDeliveryFee(), entity.getSodaPrice()), entity);
    }

    /** @param project referencia gerenciada a obra do restaurante */
    public static RestaurantEntity toEntity(Restaurant domain, ProjectEntity project) {
        if (domain == null) {
            return null;
        }
        RestaurantEntity entity = RestaurantEntity.create();
        entity.setId(domain.getId());
        apply(domain, entity, project);
        return entity;
    }

    public static void apply(Restaurant domain, RestaurantEntity entity, ProjectEntity project) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setName(domain.getName());
        entity.setPhone(domain.getPhone());
        entity.setLunchDeliveryFee(domain.getLunchDeliveryFee());
        entity.setDinnerDeliveryFee(domain.getDinnerDeliveryFee());
        entity.setSodaPrice(domain.getSodaPrice());
        entity.setProject(project);
        AuditMapping.copy(domain, entity);
        if (domain.getAddress() == null) {
            entity.setAddress(null);
        } else if (entity.getAddress() == null) {
            entity.setAddress(AddressEntityMapper.toEntity(domain.getAddress()));
        } else {
            AddressEntityMapper.apply(domain.getAddress(), entity.getAddress());
        }
    }
}
