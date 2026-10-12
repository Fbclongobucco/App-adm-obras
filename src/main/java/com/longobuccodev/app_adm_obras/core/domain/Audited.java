package com.longobuccodev.app_adm_obras.core.domain;

import java.time.Instant;

/**
 * Base dos registros de negocio auditados. A auditoria fica fora do construtor:
 * quem cria o registro chama {@link #markCreated}, quem altera chama {@link #touch},
 * e a leitura do banco chama {@link #restoreAudit}.
 */
public abstract class Audited {

    private AuditStamp audit;

    public AuditStamp getAudit() {
        return audit;
    }

    public Instant getCreatedAt() {
        return audit == null ? null : audit.createdAt();
    }

    public Instant getUpdatedAt() {
        return audit == null ? null : audit.updatedAt();
    }

    public void markCreated(Instant now) {
        this.audit = AuditStamp.createdAt(now);
    }

    /** Registra uma alteracao. Sem auditoria anterior, trata a alteracao como criacao. */
    public void touch(Instant now) {
        this.audit = audit == null ? AuditStamp.createdAt(now) : audit.touchedAt(now);
    }

    public void restoreAudit(AuditStamp audit) {
        this.audit = audit;
    }

    /** Alteracao de um registro ja existente: mantem a criacao lida e avanca a alteracao. */
    public void touchFrom(Audited previous, Instant now) {
        this.audit = previous == null || previous.audit == null
                ? AuditStamp.createdAt(now)
                : previous.audit.touchedAt(now);
    }
}
