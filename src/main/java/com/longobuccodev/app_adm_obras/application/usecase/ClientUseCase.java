package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.client.ClientRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.client.ClientResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.ClientMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class ClientUseCase {

    private final ClientRepository clientRepository;

    private final ProjectRepository projectRepository;
    private final Clock clock;

    public ClientUseCase(ClientRepository clientRepository, ProjectRepository projectRepository, Clock clock) {
        this.clientRepository = clientRepository;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    public ClientResponseDTO create(ClientRequestDTO dto) {
        Client client = ClientMapper.toDomain(dto);
        requireUniqueEmail(client);
        client.markCreated(clock.instant());
        return ClientMapper.toResponse(clientRepository.save(client));
    }

    public ClientResponseDTO findById(UUID id) {
        return ClientMapper.toResponse(findClient(id));
    }

    public ClientResponseDTO findByProjectId(UUID projectId) {
        return ClientMapper.toResponse(EntityLookup.require(projectId, clientRepository::findByProjectId,
                ResourceNotFoundException::clientByProject));
    }

    public List<ClientResponseDTO> findAll() {
        return clientRepository.findAll().stream()
                .map(ClientMapper::toResponse)
                .toList();
    }

    public PageResponseDTO<ClientResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(clientRepository.findAll(request), ClientMapper::toResponse);
    }

    /** Listagem filtrada pelo termo de busca; vazio ou nulo nao filtra. */
    public PageResponseDTO<ClientResponseDTO> findAll(PageRequest request, String query) {
        return PageMapper.toResponse(clientRepository.findAll(request, SearchTerm.of(query)), ClientMapper::toResponse);
    }

    public ClientResponseDTO update(UUID id, ClientRequestDTO dto) {
        Client existing = findClient(id);
        Client updated = ClientMapper.toDomain(existing, dto);
        requireUniqueEmail(updated);
        updated.touchFrom(existing, clock.instant());
        clientRepository.update(updated);
        return ClientMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        Client client = findClient(id);
        Conflicts.requireNone(projectRepository.findByClient(client),
                () -> ConflictException.inUse("client", "projects"));
        clientRepository.deleteById(id);
    }

    /** E-mail e opcional: so o informado precisa ser unico. */
    private void requireUniqueEmail(Client client) {
        if (client.getEmail() == null) {
            return;
        }
        Conflicts.requireUnique(clientRepository.findByEmail(client.getEmail()), Client::getId, client.getId(),
                () -> ConflictException.duplicate("client", "email", client.getEmail()));
    }

    private Client findClient(UUID id) {
        return EntityLookup.require(id, clientRepository::findById, ResourceNotFoundException::client);
    }
}
