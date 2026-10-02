package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.Address;
import com.longobuccodev.app_adm_obras.core.repository.AddressRepository;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.adapters.mapper.AddressEntityMapper;
import com.longobuccodev.app_adm_obras.infra.entities.AddressEntity;
import com.longobuccodev.app_adm_obras.infra.repositories.AddressJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class AddressRepositoryAdapter implements AddressRepository {

    private final AddressJpaRepository jpaRepository;

    public AddressRepositoryAdapter(AddressJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Address save(Address address) {
        return AddressEntityMapper.toDomain(jpaRepository.save(AddressEntityMapper.toEntity(address)));
    }

    @Override
    @Transactional(readOnly = true)
    public Address findById(UUID id) {
        return AddressEntityMapper.toDomain(jpaRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Address findByCep(String cep) {
        return AddressEntityMapper.toDomain(jpaRepository.findFirstByZipCode(cep).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Address findByStreet(String street) {
        return AddressEntityMapper.toDomain(jpaRepository.findFirstByStreet(street).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Address findByNumber(String number) {
        return AddressEntityMapper.toDomain(jpaRepository.findFirstByNumber(number).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Address findByNeighborhood(String neighborhood) {
        return AddressEntityMapper.toDomain(jpaRepository.findFirstByNeighborhood(neighborhood).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Address findByCity(String city) {
        return AddressEntityMapper.toDomain(jpaRepository.findFirstByCity(city).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Address findByState(String state) {
        return AddressEntityMapper.toDomain(jpaRepository.findFirstByState(state).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Address findByCountry(String country) {
        return AddressEntityMapper.toDomain(jpaRepository.findFirstByCountry(country).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> findAll() {
        return jpaRepository.findAll().stream().map(AddressEntityMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Address> findAll(PageRequest request) {
        return SpringPageSupport.toDomain(
                jpaRepository.findAll(SpringPageSupport.toPageable(request)), AddressEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(Address address) {
        jpaRepository.delete(AddressEntityMapper.toEntity(address));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(Address address) {
        jpaRepository.findById(address.getId())
                .ifPresent(entity -> AddressEntityMapper.apply(address, entity));
    }
}
