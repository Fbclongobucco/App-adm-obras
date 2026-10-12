package com.longobuccodev.app_adm_obras.application.dto.accommodation;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Renovacao a partir do dia seguinte ao termino; campos ausentes usam a sugestao (+1 mes, valor mensal). */
public record AccommodationRenewalRequestDTO(LocalDate endDate, BigDecimal price) {
}
