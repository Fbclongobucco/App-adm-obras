package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.AuditStamp;
import com.longobuccodev.app_adm_obras.core.domain.Audited;
import com.longobuccodev.app_adm_obras.infra.entities.AuditedEntity;

final class AuditMapping {

    private AuditMapping() {
    }

    static <T extends Audited> T restore(T domain, AuditedEntity entity) {
        if (domain != null && entity != null && entity.getCreatedAt() != null) {
            domain.restoreAudit(new AuditStamp(entity.getCreatedAt(), entity.getUpdatedAt()));
        }
        return domain;
    }

    /** Sem auditoria no dominio (escrita indireta), mantem o que a entidade ja tem. */
    static void copy(Audited domain, AuditedEntity entity) {
        if (domain == null || entity == null || domain.getAudit() == null) {
            return;
        }
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
    }
}
