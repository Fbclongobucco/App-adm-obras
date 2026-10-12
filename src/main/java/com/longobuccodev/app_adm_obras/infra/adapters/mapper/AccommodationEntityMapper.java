package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.AccommodationPeriod;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationEntity;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationPeriodEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AccommodationEntityMapper {

    private AccommodationEntityMapper() {
    }

    public static Accommodation toDomain(AccommodationEntity entity) {
        if (entity == null) {
            return null;
        }
        Accommodation accommodation = new Accommodation(entity.getId(), entity.getHostName(),
                entity.getHostPhone(), AddressEntityMapper.toDomain(entity.getAddress()),
                entity.getCapacity(), entity.getIsContract(), null, entity.getStartDate(),
                entity.getMonthlyPrice());
        accommodation.restorePeriods(entity.getPeriods().stream()
                .map(period -> AuditMapping.restore(new AccommodationPeriod(period.getId(), period.getStartDate(),
                        period.getEndDate(), period.getPrice()), period))
                .toList());
        if (entity.getEmployees() != null) {
            entity.getEmployees().forEach(employeeEntity ->
                    accommodation.addEmployee(EmployeeEntityMapper.toDomain(employeeEntity)));
        }
        return AuditMapping.restore(accommodation, entity);
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
        entity.setIsContract(domain.getIsContract());
        entity.setStartDate(domain.getStartDate());
        entity.setMonthlyPrice(domain.getMonthlyPrice());
        applyPeriods(domain, entity);
        AuditMapping.copy(domain, entity);
        if (domain.getAddress() == null) {
            entity.setAddress(null);
        } else if (entity.getAddress() == null) {
            entity.setAddress(AddressEntityMapper.toEntity(domain.getAddress()));
        } else {
            AddressEntityMapper.apply(domain.getAddress(), entity.getAddress());
        }
    }

    /** Sincroniza os periodos por id: inclui os novos, remove os ausentes e atualiza os existentes. */
    private static void applyPeriods(Accommodation domain, AccommodationEntity entity) {
        Set<UUID> kept = domain.getPeriods().stream().map(AccommodationPeriod::getId).collect(Collectors.toSet());
        entity.getPeriods().removeIf(period -> !kept.contains(period.getId()));
        Map<UUID, AccommodationPeriodEntity> current = entity.getPeriods().stream()
                .collect(Collectors.toMap(AccommodationPeriodEntity::getId, Function.identity()));
        for (AccommodationPeriod period : domain.getPeriods()) {
            AccommodationPeriodEntity target = current.get(period.getId());
            if (target == null) {
                target = AccommodationPeriodEntity.create();
                target.setId(period.getId());
                target.setAccommodation(entity);
                entity.getPeriods().add(target);
            }
            target.setStartDate(period.getStartDate());
            target.setEndDate(period.getEndDate());
            target.setPrice(period.getPrice());
            AuditMapping.copy(period, target);
        }
    }
}
