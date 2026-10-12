package com.longobuccodev.app_adm_obras.application.dto.project;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** A refeicao se vincula a obra pelo restaurante; nao ha campo de refeicao aqui. */
public record ProjectRequestDTO(
        String os,
        String description,
        UUID costCenterId,
        LocalDate startDate,
        LocalDate endDate,
        UUID clientId,
        Set<UUID> accommodationIds,
        Set<UUID> employeeIds,
        Boolean isCompleted
) {
}
