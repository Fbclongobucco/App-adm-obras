package com.longobuccodev.app_adm_obras.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccommodationSummaryDTO(
        UUID id,
        String hostName,
        String hostPhone,
        AddressResponseDTO address,
        Integer capacity,
        Integer days,
        Boolean isContract,
        BigDecimal totalPrice
) {
}
