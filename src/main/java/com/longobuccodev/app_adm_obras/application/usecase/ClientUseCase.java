package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.ClientRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.ClientResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.ClientMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;

import java.util.List;
import java.util.UUID;

public class ClientUseCase {

    private final ClientRepository clientRepository;

    public ClientUseCase(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientResponseDTO create(ClientRequestDTO dto) {
        return ClientMapper.toResponse(clientRepository.save(ClientMapper.toDomain(dto)));
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

    public ClientResponseDTO update(UUID id, ClientRequestDTO dto) {
        Client updated = ClientMapper.toDomain(findClient(id), dto);
        clientRepository.update(updated);
        return ClientMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findClient(id);
        clientRepository.deleteById(id);
    }

    private Client findClient(UUID id) {
        return EntityLookup.require(id, clientRepository::findById, ResourceNotFoundException::client);
    }
}
