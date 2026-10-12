package com.longobuccodev.app_adm_obras.application.dto.accommodation;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code endDate}: fim do ultimo periodo. {@code totalPrice}: soma dos periodos. {@code nextRenewal}:
 * sugestao da proxima renovacao (dia seguinte ao termino, +1 mes, valor mensal).
 */
public record AccommodationResponseDTO(
        UUID id,
        String hostName,
        String hostPhone,
        AddressResponseDTO address,
        Integer capacity,
        Boolean isContract,
        ProjectSummaryDTO project,
        Set<EmployeeSummaryDTO> employees,
        LocalDate startDate,
        BigDecimal monthlyPrice,
        LocalDate endDate,
        BigDecimal totalPrice,
        List<AccommodationPeriodDTO> periods,
        AccommodationPeriodDTO nextRenewal,
        Instant createdAt,
        Instant updatedAt
) {
}
