package com.longobuccodev.app_accommodation.core.domain;

import com.longobuccodev.app_accommodation.core.exception.InvalidClientException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientTest {

    private static Address newAddress() {
        return new Address(null, "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100");
    }

    private static Client newClient() {
        return new Client(null, "Construtora  Alfa ", "Contato@Alfa.com", "(11) 3333-4444",
                newAddress(), null);
    }

    @Test
    void shouldNormalizeFieldsOnConstructor() {
        Client client = newClient();

        assertThat(client.getId()).isNotNull();
        assertThat(client.getName()).isEqualTo("Construtora Alfa");
        assertThat(client.getEmail()).isEqualTo("contato@alfa.com");
        assertThat(client.getPhone()).isEqualTo("1133334444");
        assertThat(client.getProjects()).isEmpty();
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();

        assertThat(new Client(id, "Construtora Alfa", "contato@alfa.com", "1133334444", newAddress(), null)
                .getId()).isEqualTo(id);
        assertThat(newClient().getId()).isNotNull();
    }

    @Test
    void shouldCopyProjectsOnAssignment() {
        Client client = newClient();
        Set<Project> projects = new HashSet<>();
        projects.add(null);

        client.setProjects(projects);
        projects.add(new Project(null, "OS-1", "Obra", java.time.LocalDate.now(), null,
                client, null, null, false));

        assertThat(client.getProjects()).hasSize(1);
    }

    @Test
    void shouldRejectBlankName() {
        Client client = newClient();

        assertThatThrownBy(() -> client.setName(" "))
                .isInstanceOf(InvalidClientException.class)
                .extracting("errorCode").isEqualTo("client.invalid.name.blank");
    }

    @Test
    void shouldRejectShortName() {
        Client client = newClient();

        assertThatThrownBy(() -> client.setName("AB"))
                .isInstanceOf(InvalidClientException.class)
                .extracting("errorCode").isEqualTo("client.invalid.name.length");
    }

    @Test
    void shouldRejectLongName() {
        Client client = newClient();

        assertThatThrownBy(() -> client.setName("a".repeat(101)))
                .isInstanceOf(InvalidClientException.class)
                .extracting("errorCode").isEqualTo("client.invalid.name.length");
    }

    @Test
    void shouldRejectInvalidEmail() {
        Client client = newClient();

        assertThatThrownBy(() -> client.setEmail("contato@alfa"))
                .isInstanceOf(InvalidClientException.class)
                .extracting("errorCode").isEqualTo("client.invalid.email");
    }

    @Test
    void shouldRejectInvalidPhone() {
        Client client = newClient();

        assertThatThrownBy(() -> client.setPhone("3333"))
                .isInstanceOf(InvalidClientException.class)
                .extracting("errorCode").isEqualTo("client.invalid.phone");
    }

    @Test
    void shouldRejectMissingAddress() {
        Client client = newClient();

        assertThatThrownBy(() -> client.setAddress(null))
                .isInstanceOf(InvalidClientException.class)
                .extracting("errorCode").isEqualTo("client.invalid.address");
    }

    @Test
    void shouldValidateOnConstructor() {
        assertThatThrownBy(() -> new Client(null, null, "contato@alfa.com", "1133334444", newAddress(), null))
                .isInstanceOf(InvalidClientException.class);
    }
}