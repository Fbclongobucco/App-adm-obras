package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.AddressMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.Address;
import com.longobuccodev.app_adm_obras.core.repository.AddressRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;

import java.util.List;
import java.util.UUID;

public class AddressUseCase {

    private final AddressRepository addressRepository;

    public AddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public AddressResponseDTO create(AddressRequestDTO dto) {
        return AddressMapper.toResponse(addressRepository.save(AddressMapper.toDomain(dto)));
    }

    public AddressResponseDTO findById(UUID id) {
        return AddressMapper.toResponse(findAddress(id));
    }

    public List<AddressResponseDTO> findAll() {
        return addressRepository.findAll().stream()
                .map(AddressMapper::toResponse)
                .toList();
    }

    public PageResponseDTO<AddressResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(addressRepository.findAll(request), AddressMapper::toResponse);
    }

    public AddressResponseDTO update(UUID id, AddressRequestDTO dto) {
        findAddress(id);
        Address updated = AddressMapper.toDomain(id, dto);
        addressRepository.update(updated);
        return AddressMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findAddress(id);
        addressRepository.deleteById(id);
    }

    private Address findAddress(UUID id) {
        return EntityLookup.require(id, addressRepository::findById, ResourceNotFoundException::address);
    }
}
