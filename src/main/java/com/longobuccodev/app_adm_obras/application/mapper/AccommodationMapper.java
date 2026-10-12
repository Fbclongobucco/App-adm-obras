package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationPeriodDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.AccommodationPeriod;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AccommodationMapper {

    private AccommodationMapper() {
    }

    public static Accommodation toDomain(AccommodationRequestDTO dto, Project project, Set<Employee> employees) {
        Accommodation accommodation = new Accommodation(null, dto.hostName(), dto.hostPhone(),
                AddressMapper.toDomain(null, dto.address()), dto.capacity(), dto.isContract(), project,
                dto.startDate(), dto.monthlyPrice());
        addEmployees(accommodation, employees);
        return accommodation;
    }

    /**
     * Versao alterada: parte dos periodos gravados e aplica a entrada e o valor mensal pedidos pelas regras
     * da locacao (sem renovacoes, refaz o primeiro periodo; com renovacoes, a entrada nao muda).
     */
    public static Accommodation toDomain(Accommodation existing, AccommodationRequestDTO dto, Project project,
                                         Set<Employee> employees) {
        Accommodation accommodation = new Accommodation(existing.getId(), dto.hostName(), dto.hostPhone(),
                AddressMapper.toDomain(AddressMapper.idOf(existing.getAddress()), dto.address()), dto.capacity(),
                dto.isContract(), project, existing.getStartDate(), existing.getMonthlyPrice());
        accommodation.restorePeriods(existing.getPeriods());
        accommodation.configureLease(dto.startDate(), dto.monthlyPrice());
        addEmployees(accommodation, employees);
        return accommodation;
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
                accommodation.getIsContract(),
                accommodation.getStartDate(),
                accommodation.getMonthlyPrice(),
                accommodation.getEndDate(),
                accommodation.getTotalPrice(),
                periods(accommodation),
                accommodation.getCreatedAt(),
                accommodation.getUpdatedAt()
        );
    }

    public static AccommodationResponseDTO toResponse(Accommodation accommodation) {
        return new AccommodationResponseDTO(
                accommodation.getId(),
                accommodation.getHostName(),
                accommodation.getHostPhone(),
                AddressMapper.toResponse(accommodation.getAddress()),
                accommodation.getCapacity(),
                accommodation.getIsContract(),
                ProjectMapper.toSummary(accommodation.getProject()),
                accommodation.getEmployees().stream()
                        .map(EmployeeMapper::toSummary)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                accommodation.getStartDate(),
                accommodation.getMonthlyPrice(),
                accommodation.getEndDate(),
                accommodation.getTotalPrice(),
                periods(accommodation),
                toDto(accommodation.nextRenewal(), false),
                accommodation.getCreatedAt(),
                accommodation.getUpdatedAt()
        );
    }

    private static List<AccommodationPeriodDTO> periods(Accommodation accommodation) {
        return accommodation.getPeriods().stream().map(period -> toDto(period, true)).toList();
    }

    private static AccommodationPeriodDTO toDto(AccommodationPeriod period, boolean withId) {
        return new AccommodationPeriodDTO(withId ? period.getId() : null, period.getStartDate(),
                period.getEndDate(), period.getDays(), period.getPrice());
    }

    private static void addEmployees(Accommodation accommodation, Set<Employee> employees) {
        if (employees != null) {
            employees.forEach(accommodation::addEmployee);
        }
    }
}
