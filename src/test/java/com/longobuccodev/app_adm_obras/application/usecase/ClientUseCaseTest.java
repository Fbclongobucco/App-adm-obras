package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.ClientResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    private ClientRepository clientRepository;
    private ClientUseCase useCase;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        useCase = new ClientUseCase(clientRepository);
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
