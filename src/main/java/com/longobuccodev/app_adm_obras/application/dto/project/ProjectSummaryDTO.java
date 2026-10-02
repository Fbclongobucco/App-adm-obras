package com.longobuccodev.app_adm_obras.application.dto.project;

import com.longobuccodev.app_adm_obras.application.dto.client.ClientSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterResponseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectSummaryDTO(
        UUID id,
        String os,
        String description,
        CostCenterResponseDTO costCenter,
        LocalDate startDate,
        LocalDate endDate,
        ClientSummaryDTO client,
        BigDecimal totalPrice,
        Boolean isCompleted
) {
}
