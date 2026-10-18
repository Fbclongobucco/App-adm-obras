package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.LoginRecord;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(LoginAuditRepositoryAdapter.class)
class LoginAuditRepositoryAdapterTest {

    private static final Instant T0 = Instant.parse("2026-10-18T12:00:00Z");

    @Autowired
    private LoginAuditRepositoryAdapter adapter;

    @Test
    @DisplayName("Registro gravado deve ser lido com os mesmos valores")
    void shouldSaveAndRead() {
        UUID userId = UUID.randomUUID();
        LoginRecord record = new LoginRecord(UUID.randomUUID(), userId, "Ana Souza", "ana@email.com", T0,
                "203.0.113.7", "Mozilla/5.0");

        adapter.save(record);
        Page<LoginRecord> page = adapter.find(userId, null, null, new PageRequest(0, 10));

        assertThat(page.content()).singleElement().satisfies(read -> {
            assertThat(read.getId()).isEqualTo(record.getId());
            assertThat(read.getUserName()).isEqualTo("Ana Souza");
            assertThat(read.getUserEmail()).isEqualTo("ana@email.com");
            assertThat(read.getOccurredAt()).isEqualTo(T0);
            assertThat(read.getIpAddress()).isEqualTo("203.0.113.7");
            assertThat(read.getUserAgent()).isEqualTo("Mozilla/5.0");
        });
    }

    @Test
    @DisplayName("Consulta deve filtrar por periodo (from inclusivo, to exclusivo) e vir do mais recente")
    void shouldFilterByPeriodNewestFirst() {
        UUID userId = UUID.randomUUID();
        for (int hour = 0; hour < 4; hour++) {
            adapter.save(new LoginRecord(UUID.randomUUID(), userId, "Ana", "ana@email.com",
                    T0.plusSeconds(3600L * hour), null, null));
        }

        Page<LoginRecord> page = adapter.find(userId, T0.plusSeconds(3600), T0.plusSeconds(3 * 3600),
                new PageRequest(0, 10));

        assertThat(page.content()).extracting(LoginRecord::getOccurredAt)
                .containsExactly(T0.plusSeconds(2 * 3600), T0.plusSeconds(3600));
    }
}
