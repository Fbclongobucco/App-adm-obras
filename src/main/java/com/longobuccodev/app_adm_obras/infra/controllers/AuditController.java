package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.audit.ChangeAuditResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.audit.LoginAuditResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.AuditQueryUseCase;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** Consulta da auditoria. Somente ADMIN (SecurityConfig); nao ha escrita por API. */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final String PERIOD = "Instante ISO-8601 em UTC, ex.: 2026-10-01T00:00:00Z. "
            + "from inclusivo, to exclusivo; from depois de to responde 400 audit.invalid.period.";

    private final AuditQueryUseCase auditQueryUseCase;

    public AuditController(AuditQueryUseCase auditQueryUseCase) {
        this.auditQueryUseCase = auditQueryUseCase;
    }

    @Operation(summary = "Log de alteracoes",
            description = "Quem criou, alterou ou excluiu cada registro, e quando, do mais recente para o mais "
                    + "antigo. Nao traz valores de campos. Gravacao sem usuario autenticado tem actorName SYSTEM.")
    @GetMapping("/changes")
    public ResponseEntity<PageResponseDTO<ChangeAuditResponseDTO>> changes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "PROJECT, EMPLOYEE, COST_CENTER, CLIENT, ACCOMMODATION, ACCOMMODATION_PERIOD, "
                    + "MEAL, MEAL_ADJUSTMENT, RESTAURANT ou USER (400 audit.invalid.entityType)")
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @Parameter(description = "Id do usuario autor")
            @RequestParam(required = false) UUID actorId,
            @Parameter(description = PERIOD) @RequestParam(required = false) Instant from,
            @Parameter(description = PERIOD) @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(auditQueryUseCase.changes(entityType, entityId, actorId, from, to,
                new PageRequest(page, size)));
    }

    @Operation(summary = "Logins com sucesso",
            description = "Quem entrou, quando, de qual IP e com qual user-agent, do mais recente para o mais "
                    + "antigo. Falhas, refresh e logout nao sao registrados.")
    @GetMapping("/logins")
    public ResponseEntity<PageResponseDTO<LoginAuditResponseDTO>> logins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID userId,
            @Parameter(description = PERIOD) @RequestParam(required = false) Instant from,
            @Parameter(description = PERIOD) @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(auditQueryUseCase.logins(userId, from, to, new PageRequest(page, size)));
    }
}
