package com.longobuccodev.app_adm_obras.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProjectResponseDTO(
        UUID id,
        String os,
        String description,
        CostCenterResponseDTO costCenter,
        LocalDate startDate,
        LocalDate endDate,
        ClientSummaryDTO client,
        Set<AccommodationSummaryDTO> accommodations,
        Set<EmployeeSummaryDTO> employees,
        MealSummaryDTO lunch,
        MealSummaryDTO dinner,
        BigDecimal totalPrice,
        Boolean isCompleted
) {
}
