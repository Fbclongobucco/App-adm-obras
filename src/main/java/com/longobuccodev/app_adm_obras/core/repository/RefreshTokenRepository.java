package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.RefreshToken;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    RefreshToken findByHash(String tokenHash);

    /**
     * Marca o token como trocado somente se ainda nao estiver revogado.
     * Retorna {@code false} quando outra requisicao ja o revogou.
     */
    boolean markRotatedIfActive(UUID id, Instant now);

    void revokeFamily(UUID familyId, RevokeReason reason, Instant now);

    void revokeAllByUser(UUID userId, RevokeReason reason, Instant now);

    int deleteExpiredBefore(Instant instant);
}
