package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.LoginRecord;
import com.longobuccodev.app_adm_obras.core.repository.LoginAuditRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.entities.LoginAuditEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.LoginAuditJpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public class LoginAuditRepositoryAdapter implements LoginAuditRepository {

    private final LoginAuditJpaRepository jpaRepository;

    public LoginAuditRepositoryAdapter(LoginAuditJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(LoginRecord record) {
        jpaRepository.save(new LoginAuditEntity(record.getId(), record.getUserId(), record.getUserName(),
                record.getUserEmail(), record.getOccurredAt(), record.getIpAddress(), record.getUserAgent()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginRecord> find(UUID userId, Instant from, Instant to, PageRequest request) {
        Specification<LoginAuditEntity> filter = Specification.allOf(
                AuditQuerySupport.equal("userId", userId),
                AuditQuerySupport.between(from, to));
        return SpringPageSupport.toDomain(jpaRepository.findAll(filter, AuditQuerySupport.newestFirst(request)),
                LoginAuditRepositoryAdapter::toDomain);
    }

    private static LoginRecord toDomain(LoginAuditEntity entity) {
        return new LoginRecord(entity.getId(), entity.getUserId(), entity.getUserName(), entity.getUserEmail(),
                entity.getOccurredAt(), entity.getIpAddress(), entity.getUserAgent());
    }
}
