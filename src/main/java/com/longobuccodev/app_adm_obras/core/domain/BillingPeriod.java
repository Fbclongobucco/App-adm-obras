package com.longobuccodev.app_adm_obras.core.domain;

/** Periodo de fechamento com os restaurantes, em dias uteis fixos (nao segue o calendario). */
public enum BillingPeriod {
    DAY(1),
    WEEK(5),
    FORTNIGHT(10),
    MONTH(22);

    private final int workingDays;

    BillingPeriod(int workingDays) {
        this.workingDays = workingDays;
    }

    public int getWorkingDays() {
        return workingDays;
    }
}
