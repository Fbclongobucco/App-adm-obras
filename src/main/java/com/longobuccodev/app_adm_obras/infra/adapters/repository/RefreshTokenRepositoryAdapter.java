package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.RefreshToken;
import com.longobuccodev.app_adm_obras.core.domain.RefreshToken.RevokeReason;
import com.longobuccodev.app_adm_obras.core.repository.RefreshTokenRepository;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.RefreshTokenEntityMapper;
import com.longobuccodev.app_adm_obras.infra.repositories.RefreshTokenJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        return RefreshTokenEntityMapper.toDomain(jpaRepository.save(RefreshTokenEntityMapper.toEntity(token)));
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken findByHash(String tokenHash) {
        return RefreshTokenEntityMapper.toDomain(jpaRepository.findByTokenHash(tokenHash).orElse(null));
    }

    @Override
    @Transactional
    public boolean markRotatedIfActive(UUID id, Instant now) {
        return jpaRepository.revokeIfActive(id, RevokeReason.ROTATED, now) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFamilyClosed(UUID familyId) {
        return jpaRepository.existsByFamilyIdAndRevokeReasonNot(familyId, RevokeReason.ROTATED);
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId, RevokeReason reason, Instant now) {
        jpaRepository.revokeFamily(familyId, reason, now);
    }

    @Override
    @Transactional
    public void revokeAllByUser(UUID userId, RevokeReason reason, Instant now) {
        jpaRepository.revokeAllByUser(userId, reason, now);
    }

    @Override
    @Transactional
    public int deleteExpiredBefore(Instant instant) {
        return jpaRepository.deleteExpiredBefore(instant);
    }
}
