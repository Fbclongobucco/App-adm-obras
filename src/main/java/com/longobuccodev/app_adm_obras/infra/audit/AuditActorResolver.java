package com.longobuccodev.app_adm_obras.infra.audit;

import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Autor da gravacao: o usuario do token da requisicao (o subject e o id), com nome e e-mail lidos de
 * app_user na conexao da transacao. A leitura acontece uma vez por transacao. Sem autenticacao, o autor e
 * SYSTEM, sem id.
 */
final class AuditActorResolver {

    static final Actor SYSTEM = new Actor(null, ChangeAuditEntry.SYSTEM, null);

    private static final Object CACHE_KEY = AuditActorResolver.class;

    private AuditActorResolver() {
    }

    static Actor resolve(Connection connection) throws SQLException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM;
        }
        UUID userId = parse(authentication.getName());
        if (userId == null) {
            // Autenticacao sem id de usuario (nao vem do token da API): registra o nome que ela traz
            return new Actor(null, authentication.getName(), null);
        }
        Actor cached = cached(userId);
        if (cached != null) {
            return cached;
        }
        Actor actor = load(connection, userId);
        cache(actor);
        return actor;
    }

    private static Actor load(Connection connection, UUID userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select name, email from app_user where id = ?")) {
            statement.setObject(1, userId);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    return new Actor(userId, row.getString(1), row.getString(2));
                }
            }
        }
        // Token valido de usuario ja excluido: o id identifica o autor
        return new Actor(userId, userId.toString(), null);
    }

    private static Actor cached(UUID userId) {
        if (TransactionSynchronizationManager.getResource(CACHE_KEY) instanceof Actor actor
                && userId.equals(actor.id())) {
            return actor;
        }
        return null;
    }

    private static void cache(Actor actor) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.unbindResourceIfPossible(CACHE_KEY);
        TransactionSynchronizationManager.bindResource(CACHE_KEY, actor);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                TransactionSynchronizationManager.unbindResourceIfPossible(CACHE_KEY);
            }
        });
    }

    private static UUID parse(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    record Actor(UUID id, String name, String email) {
    }
}
