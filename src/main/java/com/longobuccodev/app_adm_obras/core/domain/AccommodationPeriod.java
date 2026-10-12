package com.longobuccodev.app_adm_obras.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Periodo de locacao da hospedagem: o primeiro comeca na entrada, e cada renovacao comeca no dia seguinte
 * ao fim do anterior. As regras ficam em {@link Accommodation}, que cria os periodos.
 */
public class AccommodationPeriod extends Audited {

    private final UUID id;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal price;

    public AccommodationPeriod(UUID id, LocalDate startDate, LocalDate endDate, BigDecimal price) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    /** Dias corridos, contando o inicio e o fim. */
    public int getDays() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** Mesmo id e auditoria com outras datas e valor (edicao do primeiro periodo). */
    AccommodationPeriod with(LocalDate start, LocalDate end, BigDecimal newPrice) {
        AccommodationPeriod copy = new AccommodationPeriod(id, start, end, newPrice);
        copy.restoreAudit(getAudit());
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof AccommodationPeriod period && Objects.equals(id, period.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
