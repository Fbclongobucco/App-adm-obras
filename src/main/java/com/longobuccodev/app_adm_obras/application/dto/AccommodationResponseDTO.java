package com.longobuccodev.app_adm_obras.application.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record AccommodationResponseDTO(
        UUID id,
        String hostName,
        String hostPhone,
        AddressResponseDTO address,
        Integer capacity,
        Integer days,
        Boolean isContract,
        ProjectSummaryDTO project,
        Set<EmployeeSummaryDTO> employees,
        BigDecimal totalPrice
) {
}
