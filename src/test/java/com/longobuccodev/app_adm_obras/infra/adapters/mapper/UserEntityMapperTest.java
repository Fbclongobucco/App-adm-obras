package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.exception.InvalidUserException;
import com.longobuccodev.app_adm_obras.infra.entities.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEntityMapperTest {

    @Test
    @DisplayName("toDomain deve converter id, nome normalizado, email em minusculas, ativo e roles")
    void toDomainShouldMapEveryField() {
        UUID id = UUID.randomUUID();
        UserEntity entity = UserEntity.create();
        entity.setId(id);
        entity.setName("Maria Souza");
        entity.setEmail("maria@email.com");
        entity.setIsActive(true);
        entity.setPasswordHash("$2a$10$hashAdmin");
        entity.setRoles(EnumSet.of(Role.ADMIN, Role.OPERADOR));

        User user = UserEntityMapper.toDomain(entity);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getName()).isEqualTo("Maria Souza");
        assertThat(user.getEmail()).isEqualTo("maria@email.com");
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.OPERADOR);
    }

    @Test
    @DisplayName("toDomain deve retornar null quando a entidade for null")
    void toDomainShouldReturnNullForNullEntity() {
        assertThat(UserEntityMapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("toDomain deve retornar roles vazios quando a entidade nao tiver nenhuma role")
    void toDomainShouldReturnEmptyRolesWhenNonePersisted() {
        UserEntity entity = UserEntity.create();
        entity.setName("Joao da Silva");
        entity.setEmail("joao@email.com");
        entity.setIsActive(true);
        entity.setPasswordHash("$2a$10$hashJoao");
        entity.setRoles(null);

        assertThat(UserEntityMapper.toDomain(entity).getRoles()).isEmpty();
    }

    @Test
    @DisplayName("toEntity deve copiar todos os campos e as roles do dominio")
    void toEntityShouldMapEveryField() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "Ana Lima", "ANA@email.com", true, "$2a$10$hashAna");
        user.addRole(Role.OPERADOR);

        UserEntity entity = UserEntityMapper.toEntity(user);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Ana Lima");
        assertThat(entity.getEmail()).isEqualTo("ana@email.com");
        assertThat(entity.getIsActive()).isTrue();
        assertThat(entity.getPasswordHash()).isEqualTo("$2a$10$hashAna");
        assertThat(entity.getRoles()).containsExactly(Role.OPERADOR);
    }

    @Test
    @DisplayName("toEntity deve retornar null quando o dominio for null")
    void toEntityShouldReturnNullForNullDomain() {
        assertThat(UserEntityMapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("apply deve sobrescrever os campos existentes preservando o id")
    void applyShouldOverwriteFieldsKeepingId() {
        UUID id = UUID.randomUUID();
        UserEntity entity = UserEntity.create();
        entity.setId(id);
        entity.setName("Nome Antigo");
        entity.setEmail("antigo@email.com");
        entity.setIsActive(true);
        entity.setRoles(EnumSet.of(Role.ADMIN));

        User user = new User(id, "Nome Novo", "novo@email.com", false, "$2a$10$hashNovo");
        user.addRole(Role.OPERADOR);
        UserEntityMapper.apply(user, entity);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Nome Novo");
        assertThat(entity.getEmail()).isEqualTo("novo@email.com");
        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getPasswordHash()).isEqualTo("$2a$10$hashNovo");
        assertThat(entity.getRoles()).containsExactly(Role.OPERADOR);
    }

    @Test
    @DisplayName("toDomain deve rejeitar entidade persistida sem senha")
    void toDomainShouldRejectEntityWithoutPassword() {
        UserEntity entity = UserEntity.create();
        entity.setId(UUID.randomUUID());
        entity.setName("Sem Senha");
        entity.setEmail("sem@email.com");
        entity.setIsActive(true);

        assertThatThrownBy(() -> UserEntityMapper.toDomain(entity))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("apply nao deve lancar excecao quando o dominio ou a entidade forem null")
    void applyShouldIgnoreNullArguments() {
        UserEntity entity = UserEntity.create();
        User user = new User(null, "Ana Lima", "ana@email.com", true, "$2a$10$hashAna");

        UserEntityMapper.apply(null, entity);
        UserEntityMapper.apply(user, null);
        UserEntityMapper.apply(null, null);

        assertThat(entity.getName()).isNull();
    }
}
