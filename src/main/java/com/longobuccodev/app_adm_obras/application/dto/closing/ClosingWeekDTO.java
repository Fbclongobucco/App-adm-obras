package com.longobuccodev.app_adm_obras.application.dto.closing;

import java.time.LocalDate;

/** Semana de fechamento (segunda a domingo) cortada no periodo da obra. */
public record ClosingWeekDTO(LocalDate start, LocalDate end, boolean partial, boolean current) {
}
