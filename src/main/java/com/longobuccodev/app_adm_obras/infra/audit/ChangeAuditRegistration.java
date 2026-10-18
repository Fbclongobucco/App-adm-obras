package com.longobuccodev.app_adm_obras.infra.audit;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Registra o {@link ChangeAuditListener} na SessionFactory na subida da aplicacao. */
@Component
class ChangeAuditRegistration {

    ChangeAuditRegistration(EntityManagerFactory entityManagerFactory, Clock clock) {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry().requireService(EventListenerRegistry.class);
        ChangeAuditListener listener = new ChangeAuditListener(clock);
        registry.appendListeners(EventType.POST_INSERT, listener);
        registry.appendListeners(EventType.POST_UPDATE, listener);
        registry.appendListeners(EventType.POST_DELETE, listener);
    }
}
