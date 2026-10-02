package com.longobuccodev.app_adm_obras.application.dto.project;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProjectRequestDTO(
        String os,
        String description,
        UUID costCenterId,
        LocalDate startDate,
        LocalDate endDate,
        UUID clientId,
        Set<UUID> accommodationIds,
        Set<UUID> employeeIds,
        UUID lunchId,
        UUID dinnerId,
        Boolean isCompleted
) {
}
