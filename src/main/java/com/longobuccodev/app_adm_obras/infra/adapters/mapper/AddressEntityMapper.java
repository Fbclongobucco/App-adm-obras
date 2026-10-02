package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Address;
import com.longobuccodev.app_adm_obras.infra.entities.AddressEntity;

public final class AddressEntityMapper {

    private AddressEntityMapper() {
    }

    public static Address toDomain(AddressEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Address(entity.getId(), entity.getStreet(), entity.getNumber(), entity.getCity(),
                entity.getState(), entity.getCountry(), entity.getNeighborhood(), entity.getZipCode());
    }

    public static AddressEntity toEntity(Address domain) {
        if (domain == null) {
            return null;
        }
        AddressEntity entity = AddressEntity.create();
        entity.setId(domain.getId());
        entity.setStreet(domain.getStreet());
        entity.setNumber(domain.getNumber());
        entity.setCity(domain.getCity());
        entity.setState(domain.getState());
        entity.setCountry(domain.getCountry());
        entity.setNeighborhood(domain.getNeighborhood());
        entity.setZipCode(domain.getZipCode());
        return entity;
    }

    public static void apply(Address domain, AddressEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setStreet(domain.getStreet());
        entity.setNumber(domain.getNumber());
        entity.setCity(domain.getCity());
        entity.setState(domain.getState());
        entity.setCountry(domain.getCountry());
        entity.setNeighborhood(domain.getNeighborhood());
        entity.setZipCode(domain.getZipCode());
    }
}
