package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.LoginRecord;

import java.time.Instant;
import java.util.UUID;

public interface LoginAuditRepository {

    void save(LoginRecord record);

    /**
     * Registros do mais recente para o mais antigo. Filtros nulos nao filtram; {@code from} inclusivo,
     * {@code to} exclusivo.
     */
    Page<LoginRecord> find(UUID userId, Instant from, Instant to, PageRequest request);
}
