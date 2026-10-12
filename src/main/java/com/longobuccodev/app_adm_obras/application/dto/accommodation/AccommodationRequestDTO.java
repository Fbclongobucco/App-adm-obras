package com.longobuccodev.app_adm_obras.application.dto.accommodation;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Hospedagem locada por mes. {@code startDate} e {@code monthlyPrice} definem o primeiro periodo; com
 * renovacoes, a entrada nao muda e o valor mensal vale so como sugestao das proximas renovacoes.
 */
public record AccommodationRequestDTO(
        String hostName,
        String hostPhone,
        AddressRequestDTO address,
        Integer capacity,
        Boolean isContract,
        UUID projectId,
        Set<UUID> employeeIds,
        LocalDate startDate,
        BigDecimal monthlyPrice
) {
}
