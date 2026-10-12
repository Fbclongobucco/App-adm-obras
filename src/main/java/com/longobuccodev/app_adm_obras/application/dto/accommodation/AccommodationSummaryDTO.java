package com.longobuccodev.app_adm_obras.application.dto.accommodation;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code endDate}: fim do ultimo periodo. {@code totalPrice}: soma dos periodos. */
public record AccommodationSummaryDTO(
        UUID id,
        String hostName,
        String hostPhone,
        AddressResponseDTO address,
        Integer capacity,
        Boolean isContract,
        LocalDate startDate,
        BigDecimal monthlyPrice,
        LocalDate endDate,
        BigDecimal totalPrice,
        List<AccommodationPeriodDTO> periods,
        Instant createdAt,
        Instant updatedAt
) {
}
