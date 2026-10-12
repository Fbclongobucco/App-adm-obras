package com.longobuccodev.app_adm_obras.infra.security;

import com.longobuccodev.app_adm_obras.core.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;

/**
 * Remove diariamente os refresh tokens expirados. Expirados ja sao recusados
 * no refresh, inclusive para detectar reuso, entao nao ha o que preservar.
 */
@Configuration
@EnableScheduling
public class RefreshTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.security.refresh-token-cleanup-cron:0 0 3 * * *}", zone = "UTC")
    public int purgeExpired() {
        int removed = refreshTokenRepository.deleteExpiredBefore(clock.instant());
        log.info("Removed {} expired refresh tokens", removed);
        return removed;
    }
}
