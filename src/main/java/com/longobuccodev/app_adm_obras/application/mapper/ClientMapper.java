package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.ClientRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.ClientResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.ClientSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Client;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public final class ClientMapper {

    private ClientMapper() {
    }

    public static Client toDomain(ClientRequestDTO dto) {
        return new Client(null, dto.name(), dto.email(), dto.phone(), AddressMapper.toDomain(dto.address()));
    }

    public static Client toDomain(Client existing, ClientRequestDTO dto) {
        Client client = new Client(existing.getId(), dto.name(), dto.email(), dto.phone(),
                AddressMapper.toDomain(AddressMapper.idOf(existing.getAddress()), dto.address()));
        existing.getProjects().forEach(client::addProject);
        return client;
    }

    public static ClientSummaryDTO toSummary(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientSummaryDTO(client.getId(), client.getName(), client.getEmail(), client.getPhone(),
                AddressMapper.toResponse(client.getAddress()));
    }

    public static ClientResponseDTO toResponse(Client client) {
        return new ClientResponseDTO(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                AddressMapper.toResponse(client.getAddress()),
                client.getProjects().stream()
                        .map(ProjectMapper::toSummary)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }
}
