package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.AccommodationRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.AccommodationResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.AccommodationSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AccommodationMapper {

    private AccommodationMapper() {
    }

    public static Accommodation toDomain(AccommodationRequestDTO dto, Project project, Set<Employee> employees) {
        return toDomain(null, null, dto, project, employees);
    }

    public static Accommodation toDomain(Accommodation existing, AccommodationRequestDTO dto, Project project,
                                         Set<Employee> employees) {
        return toDomain(existing.getId(), AddressMapper.idOf(existing.getAddress()), dto, project, employees);
    }

    public static AccommodationSummaryDTO toSummary(Accommodation accommodation) {
        if (accommodation == null) {
            return null;
        }
        return new AccommodationSummaryDTO(
                accommodation.getId(),
                accommodation.getHostName(),
                accommodation.getHostPhone(),
                AddressMapper.toResponse(accommodation.getAddress()),
                accommodation.getCapacity(),
                accommodation.getDays(),
                accommodation.getIsContract(),
                accommodation.getTotalPrice()
        );
    }

    public static AccommodationResponseDTO toResponse(Accommodation accommodation) {
        return new AccommodationResponseDTO(
                accommodation.getId(),
                accommodation.getHostName(),
                accommodation.getHostPhone(),
                AddressMapper.toResponse(accommodation.getAddress()),
                accommodation.getCapacity(),
                accommodation.getDays(),
                accommodation.getIsContract(),
                ProjectMapper.toSummary(accommodation.getProject()),
                accommodation.getEmployees().stream()
                        .map(EmployeeMapper::toSummary)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                accommodation.getTotalPrice()
        );
    }

    private static Accommodation toDomain(UUID id, UUID addressId, AccommodationRequestDTO dto, Project project,
                                          Set<Employee> employees) {
        Accommodation accommodation = new Accommodation(
                id,
                dto.hostName(),
                dto.hostPhone(),
                AddressMapper.toDomain(addressId, dto.address()),
                dto.capacity(),
                dto.days(),
                dto.isContract(),
                project,
                dto.totalPrice()
        );
        if (employees != null) {
            employees.forEach(accommodation::addEmployee);
        }
        return accommodation;
    }
}
