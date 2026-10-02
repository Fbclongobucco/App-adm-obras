package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.exception.InvalidUserException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static User newUser() {
        return new User(null, "Ana  Souza ", "Ana.Souza@Email.com ", true);
    }

    @Test
    void shouldNormalizeFieldsOnConstructor() {
        User user = newUser();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getName()).isEqualTo("Ana Souza");
        assertThat(user.getEmail()).isEqualTo("ana.souza@email.com");
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();

        assertThat(new User(id, "Ana Souza", "ana.souza@email.com", true).getId()).isEqualTo(id);
        assertThat(newUser().getId()).isNotNull();
    }

    @Test
    void shouldAddRemoveAndCheckRoles() {
        User user = newUser();

        user.addRole(Role.ADMIN);
        user.addRole(Role.OPERADOR);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.OPERADOR);
        assertThat(user.hasRole(Role.ADMIN)).isTrue();

        user.removeRole(Role.ADMIN);

        assertThat(user.getRoles()).containsExactly(Role.OPERADOR);
        assertThat(user.hasRole(Role.ADMIN)).isFalse();
    }

    @Test
    void shouldIgnoreDuplicatedRole() {
        User user = newUser();

        user.addRole(Role.OPERADOR);
        user.addRole(Role.OPERADOR);

        assertThat(user.getRoles()).hasSize(1);
    }

    @Test
    void shouldExposeReadOnlyRoles() {
        User user = newUser();

        assertThatThrownBy(() -> user.getRoles().add(Role.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectNullRole() {
        User user = newUser();

        assertThatThrownBy(() -> user.addRole(null))
                .isInstanceOf(InvalidUserException.class)
                .extracting("errorCode").isEqualTo("user.invalid.role");
    }

    @Test
    void shouldRejectBlankName() {
        User user = newUser();

        assertThatThrownBy(() -> user.setName(" "))
                .isInstanceOf(InvalidUserException.class)
                .extracting("errorCode").isEqualTo("user.invalid.name.blank");
    }

    @Test
    void shouldRejectShortName() {
        User user = newUser();

        assertThatThrownBy(() -> user.setName("An"))
                .isInstanceOf(InvalidUserException.class)
                .extracting("errorCode").isEqualTo("user.invalid.name.length");
    }

    @Test
    void shouldRejectLongName() {
        User user = newUser();

        assertThatThrownBy(() -> user.setName("a".repeat(101)))
                .isInstanceOf(InvalidUserException.class)
                .extracting("errorCode").isEqualTo("user.invalid.name.length");
    }

    @Test
    void shouldRejectInvalidEmail() {
        User user = newUser();

        assertThatThrownBy(() -> user.setEmail("ana.souza"))
                .isInstanceOf(InvalidUserException.class)
                .extracting("errorCode").isEqualTo("user.invalid.email");
    }

    @Test
    void shouldDefaultActiveToFalse() {
        User user = newUser();

        user.setActive(null);

        assertThat(user.isActive()).isFalse();
        assertThat(user.getIsActive()).isFalse();
    }

    @Test
    void shouldCompareById() {
        UUID id = UUID.randomUUID();
        User first = newUser();
        User second = newUser();

        assertThat(first).isNotEqualTo(second);

        first.setId(id);
        second.setId(id);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldValidateOnConstructor() {
        assertThatThrownBy(() -> new User(null, null, "ana.souza@email.com", true))
                .isInstanceOf(InvalidUserException.class);
        assertThatThrownBy(() -> new User(null, "Ana Souza", "invalido", true))
                .isInstanceOf(InvalidUserException.class);
    }
}
