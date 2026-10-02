package com.longobuccodev.app_adm_obras.application.dto.project;

import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.client.ClientSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealSummaryDTO;
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
