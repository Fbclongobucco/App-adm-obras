package com.longobuccodev.app_adm_obras.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Fechamento de um dia de uma refeicao ou, somado, de um restaurante.
 *
 * @param partialNoService no restaurante: parte das refeicoes sem fornecimento num dia em que outra fornece
 * @param adjustmentIds    ajustes por data que definem o dia (dia extra ou sem fornecimento)
 */
public record DayClosing(
        LocalDate date,
        Status status,
        boolean partialNoService,
        int lunchMeals,
        int dinnerMeals,
        int deliveries,
        int sodas,
        BigDecimal price,
        List<UUID> adjustmentIds
) {

    public enum Status {
        /** Dia util com o fornecimento contratado. */
        SERVICE,
        /** Dia util sem fornecimento (ajuste). */
        NO_SERVICE,
        /** Sabado ou domingo com fornecimento (ajuste). */
        EXTRA_DAY,
        /** Sabado ou domingo sem ajuste. */
        OFF,
        /** Antes do inicio ou depois do termino da obra. */
        OUT_OF_PERIOD
    }

    public static DayClosing empty(LocalDate date, Status status) {
        return new DayClosing(date, status, false, 0, 0, 0, 0, BigDecimal.ZERO, List.of());
    }

    /** Dia que fornece algo: fornecimento normal ou dia extra. */
    public boolean isServiceDay() {
        return status == Status.SERVICE || status == Status.EXTRA_DAY;
    }
}
