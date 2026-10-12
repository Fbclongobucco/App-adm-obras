package com.longobuccodev.app_adm_obras.application.dto.accommodation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Periodo de locacao; {@code days} conta o inicio e o fim. {@code id} nulo na sugestao de renovacao. */
public record AccommodationPeriodDTO(UUID id, LocalDate startDate, LocalDate endDate, int days, BigDecimal price) {
}
