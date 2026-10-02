package com.longobuccodev.app_adm_obras.application.dto.accommodation;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeSummaryDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectSummaryDTO;
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
