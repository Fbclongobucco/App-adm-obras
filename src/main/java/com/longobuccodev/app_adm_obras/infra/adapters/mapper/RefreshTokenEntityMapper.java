package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.RefreshToken;
import com.longobuccodev.app_adm_obras.infra.entities.RefreshTokenEntity;

public final class RefreshTokenEntityMapper {

    private RefreshTokenEntityMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RefreshToken(entity.getId(), entity.getUserId(), entity.getTokenHash(), entity.getFamilyId(),
                entity.getExpiresAt(), entity.getCreatedAt(), entity.getRevokedAt(), entity.getRevokeReason());
    }

    public static RefreshTokenEntity toEntity(RefreshToken token) {
        RefreshTokenEntity entity = RefreshTokenEntity.create();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setTokenHash(token.getTokenHash());
        entity.setFamilyId(token.getFamilyId());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setCreatedAt(token.getCreatedAt());
        entity.setRevokedAt(token.getRevokedAt());
        entity.setRevokeReason(token.getRevokeReason());
        return entity;
    }
}
