package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.client.ClientRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.client.ClientResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.client;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.clientRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientUseCaseTest {

    static final Instant CLOCK_NOW = Instant.parse("2026-01-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(CLOCK_NOW, ZoneOffset.UTC);

    private ClientRepository clientRepository;
    private ClientUseCase useCase;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        useCase = new ClientUseCase(clientRepository, mock(ProjectRepository.class), CLOCK);
    }

    @Test
    void shouldCreateClient() {
        when(clientRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponseDTO response = useCase.create(clientRequest());

        assertThat(response.id()).isNotNull();
        assertThat(response.address().state()).isEqualTo("SP");
        assertThat(response.projects()).isEmpty();
    }

    @Test
    void shouldCreateClientWithoutEmailWithoutCheckingDuplicates() {
        when(clientRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ClientRequestDTO request = clientRequest();
        ClientRequestDTO withoutEmail = new ClientRequestDTO(request.name(), " ", request.phone(), request.address());

        ClientResponseDTO response = useCase.create(withoutEmail);

        assertThat(response.email()).isNull();
        verify(clientRepository, never()).findByEmail(any());
    }

    @Test
    void shouldRejectDuplicateInformedEmail() {
        when(clientRepository.findByEmail("contato@alfa.com")).thenReturn(client());

        assertThatThrownBy(() -> useCase.create(clientRequest()))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("client.duplicate.email");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void shouldUpdateKeepingId() {
        Client existing = client();
        when(clientRepository.findById(existing.getId())).thenReturn(existing);

        ClientResponseDTO response = useCase.update(existing.getId(), clientRequest());

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(clientRepository).update(any(Client.class));
    }

    @Test
    void shouldRejectFindByProjectWithoutClient() {
        assertThatThrownBy(() -> useCase.findByProjectId(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("client.not_found");
    }

    @Test
    void shouldRejectDeleteOfUnknownClient() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(clientRepository, never()).deleteById(any());
    }
}
