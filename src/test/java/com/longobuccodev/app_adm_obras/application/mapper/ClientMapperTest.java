package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.ClientResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import org.junit.jupiter.api.Test;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.address;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.clientRequest;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static org.assertj.core.api.Assertions.assertThat;

class ClientMapperTest {

    @Test
    void shouldKeepIdAddressAndProjectsWhenMappingOverExisting() {
        Project project = project();
        Client existing = new Client(null, "Construtora Alfa", "contato@alfa.com", "1133334444", address());
        existing.addProject(project);

        Client updated = ClientMapper.toDomain(existing, clientRequest());

        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getAddress().getId()).isEqualTo(existing.getAddress().getId());
        assertThat(updated.getProjects()).containsExactly(project);
    }

    @Test
    void shouldMapDomainToResponseWithProjects() {
        Project project = project();
        Client client = new Client(null, "Construtora Alfa", "contato@alfa.com", "1133334444", address());
        client.addProject(project);

        ClientResponseDTO response = ClientMapper.toResponse(client);

        assertThat(response.address().zipCode()).isEqualTo("01310100");
        assertThat(response.projects()).singleElement()
                .satisfies(summary -> assertThat(summary.os()).isEqualTo("OS-1234"));
    }
}
