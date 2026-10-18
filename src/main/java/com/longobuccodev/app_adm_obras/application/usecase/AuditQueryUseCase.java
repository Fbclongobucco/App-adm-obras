package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.audit.ChangeAuditResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.audit.LoginAuditResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.EntityType;
import com.longobuccodev.app_adm_obras.core.domain.LoginRecord;
import com.longobuccodev.app_adm_obras.core.exception.InvalidAuditFilterException;
import com.longobuccodev.app_adm_obras.core.repository.ChangeAuditRepository;
import com.longobuccodev.app_adm_obras.core.repository.LoginAuditRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/** Consulta da auditoria de alteracoes e de login, do mais recente para o mais antigo. */
public class AuditQueryUseCase {

    private final ChangeAuditRepository changeAuditRepository;
    private final LoginAuditRepository loginAuditRepository;

    public AuditQueryUseCase(ChangeAuditRepository changeAuditRepository, LoginAuditRepository loginAuditRepository) {
        this.changeAuditRepository = changeAuditRepository;
        this.loginAuditRepository = loginAuditRepository;
    }

    /** Filtros nulos nao filtram; {@code from} inclusivo, {@code to} exclusivo. */
    public PageResponseDTO<ChangeAuditResponseDTO> changes(String entityType, UUID entityId, UUID actorId,
                                                           Instant from, Instant to, PageRequest request) {
        EntityType type = entityType(entityType);
        requireValidPeriod(from, to);
        return PageMapper.toResponse(changeAuditRepository.find(type, entityId, actorId, from, to, request),
                AuditQueryUseCase::toResponse);
    }

    /** Filtros nulos nao filtram; {@code from} inclusivo, {@code to} exclusivo. */
    public PageResponseDTO<LoginAuditResponseDTO> logins(UUID userId, Instant from, Instant to, PageRequest request) {
        requireValidPeriod(from, to);
        return PageMapper.toResponse(loginAuditRepository.find(userId, from, to, request),
                AuditQueryUseCase::toResponse);
    }

    private static EntityType entityType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(EntityType.values()).filter(type -> type.name().equals(value)).findFirst()
                .orElseThrow(() -> InvalidAuditFilterException.unknownEntityType(value));
    }

    private static void requireValidPeriod(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw InvalidAuditFilterException.invalidPeriod(from, to);
        }
    }

    private static ChangeAuditResponseDTO toResponse(ChangeAuditEntry entry) {
        return new ChangeAuditResponseDTO(entry.getId(), entry.getEntityType().name(), entry.getEntityId(),
                entry.getAction().name(), entry.getOccurredAt(), entry.getActorId(), entry.getActorName(),
                entry.getActorEmail());
    }

    private static LoginAuditResponseDTO toResponse(LoginRecord record) {
        return new LoginAuditResponseDTO(record.getId(), record.getUserId(), record.getUserName(),
                record.getUserEmail(), record.getOccurredAt(), record.getIpAddress(), record.getUserAgent());
    }
}
