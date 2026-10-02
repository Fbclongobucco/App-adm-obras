package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.infra.entities.ClientEntity;

public final class ClientEntityMapper {

    private ClientEntityMapper() {
    }

    public static Client toDomain(ClientEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Client(entity.getId(), entity.getName(), entity.getEmail(), entity.getPhone(),
                AddressEntityMapper.toDomain(entity.getAddress()));
    }

    public static ClientEntity toEntity(Client domain) {
        if (domain == null) {
            return null;
        }
        ClientEntity entity = ClientEntity.create();
        entity.setId(domain.getId());
        entity.setAddress(AddressEntityMapper.toEntity(domain.getAddress()));
        apply(domain, entity);
        return entity;
    }

    public static void apply(Client domain, ClientEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        if (domain.getAddress() == null) {
            entity.setAddress(null);
            return;
        }
        if (entity.getAddress() == null) {
            entity.setAddress(AddressEntityMapper.toEntity(domain.getAddress()));
        } else {
            AddressEntityMapper.apply(domain.getAddress(), entity.getAddress());
        }
    }
}
