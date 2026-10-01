package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.core.domain.Address;

import java.util.UUID;

public final class AddressMapper {

    private AddressMapper() {
    }

    public static Address toDomain(AddressRequestDTO dto) {
        return toDomain(null, dto);
    }

    public static Address toDomain(UUID id, AddressRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Address(id, dto.street(), dto.number(), dto.city(), dto.state(), dto.country(),
                dto.neighborhood(), dto.zipCode());
    }

    public static UUID idOf(Address address) {
        return address == null ? null : address.getId();
    }

    public static AddressResponseDTO toResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponseDTO(address.getId(), address.getStreet(), address.getNumber(),
                address.getCity(), address.getState(), address.getCountry(), address.getNeighborhood(),
                address.getZipCode());
    }
}
