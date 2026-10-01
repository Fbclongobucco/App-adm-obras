package com.longobuccodev.app_adm_obras.application.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record AccommodationRequestDTO(
        String hostName,
        String hostPhone,
        AddressRequestDTO address,
        Integer capacity,
        Integer days,
        Boolean isContract,
        UUID projectId,
        Set<UUID> employeeIds,
        BigDecimal totalPrice
) {
}
