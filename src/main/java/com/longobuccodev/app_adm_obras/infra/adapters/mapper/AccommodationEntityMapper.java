package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationEntity;

public final class AccommodationEntityMapper {

    private AccommodationEntityMapper() {
    }

    public static Accommodation toDomain(AccommodationEntity entity) {
        if (entity == null) {
            return null;
        }
        Accommodation accommodation = new Accommodation(entity.getId(), entity.getHostName(),
                entity.getHostPhone(), AddressEntityMapper.toDomain(entity.getAddress()),
                entity.getCapacity(), entity.getDays(), entity.getIsContract(), null,
                entity.getTotalPrice());
        if (entity.getEmployees() != null) {
            entity.getEmployees().forEach(employeeEntity ->
                    accommodation.addEmployee(EmployeeEntityMapper.toDomain(employeeEntity)));
        }
        return accommodation;
    }

    public static AccommodationEntity toEntity(Accommodation domain) {
        if (domain == null) {
            return null;
        }
        AccommodationEntity entity = AccommodationEntity.create();
        entity.setId(domain.getId());
        apply(domain, entity);
        return entity;
    }

    public static void apply(Accommodation domain, AccommodationEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setHostName(domain.getHostName());
        entity.setHostPhone(domain.getHostPhone());
        entity.setCapacity(domain.getCapacity());
        entity.setDays(domain.getDays());
        entity.setIsContract(domain.getIsContract());
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
