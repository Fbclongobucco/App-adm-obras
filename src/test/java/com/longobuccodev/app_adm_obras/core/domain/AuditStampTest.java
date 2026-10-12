package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidAuditStampException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditStampTest {

    private static final Instant T0 = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    @Test
    @DisplayName("criacao sem instante deve ser rejeitada")
    void shouldRejectMissingCreatedAt() {
        assertThatThrownBy(() -> new AuditStamp(null, T0))
                .isInstanceOf(InvalidAuditStampException.class)
                .hasFieldOrPropertyWithValue("errorCode", "audit.invalid.created_at");
    }

    @Test
    @DisplayName("alteracao sem instante deve ser rejeitada")
    void shouldRejectMissingUpdatedAt() {
        assertThatThrownBy(() -> new AuditStamp(T0, null))
                .isInstanceOf(InvalidAuditStampException.class)
                .hasFieldOrPropertyWithValue("errorCode", "audit.invalid.updated_at");
    }

    @Test
    @DisplayName("alteracao anterior a criacao deve ser rejeitada")
    void shouldRejectUpdateBeforeCreation() {
        assertThatThrownBy(() -> new AuditStamp(T1, T0))
                .isInstanceOf(InvalidAuditStampException.class)
                .hasFieldOrPropertyWithValue("errorCode", "audit.invalid.updated_at");
    }

    @Test
    @DisplayName("registro criado deve ter criacao e alteracao iguais")
    void markCreatedShouldSetBothTimestamps() {
        CostCenter costCenter = new CostCenter(null, "Centro Norte", "11222333000181");

        costCenter.markCreated(T0);

        assertThat(costCenter.getCreatedAt()).isEqualTo(T0);
        assertThat(costCenter.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("touch deve avancar so a alteracao")
    void touchShouldKeepCreationTimestamp() {
        CostCenter costCenter = new CostCenter(null, "Centro Norte", "11222333000181");
        costCenter.markCreated(T0);

        costCenter.touch(T1);

        assertThat(costCenter.getCreatedAt()).isEqualTo(T0);
        assertThat(costCenter.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    @DisplayName("touchFrom deve herdar a criacao do registro anterior")
    void touchFromShouldInheritCreationTimestamp() {
        CostCenter previous = new CostCenter(null, "Centro Norte", "11222333000181");
        previous.markCreated(T0);
        CostCenter replacement = new CostCenter(previous.getId(), "Centro Sul", "11222333000181");

        replacement.touchFrom(previous, T1);

        assertThat(replacement.getCreatedAt()).isEqualTo(T0);
        assertThat(replacement.getUpdatedAt()).isEqualTo(T1);
        assertThat(previous.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("instantes devem ser truncados para microssegundos, a precisao do banco")
    void shouldTruncateToMicroseconds() {
        AuditStamp stamp = AuditStamp.createdAt(Instant.parse("2026-01-15T10:00:00.123456789Z"));

        assertThat(stamp.createdAt()).isEqualTo(Instant.parse("2026-01-15T10:00:00.123456Z"));
        assertThat(stamp.updatedAt()).isEqualTo(Instant.parse("2026-01-15T10:00:00.123456Z"));
    }
}
