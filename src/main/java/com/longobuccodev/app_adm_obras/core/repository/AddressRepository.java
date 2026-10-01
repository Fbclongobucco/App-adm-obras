package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Address;

import java.util.List;
import java.util.UUID;

public interface AddressRepository {

    Address save(Address address);
    Address findById(UUID id);
    Address findByCep(String cep);
    Address findByStreet(String street);
    Address findByNumber(String number);
    Address findByComplement(String complement);
    Address findByNeighborhood(String neighborhood);
    Address findByCity(String city);
    Address findByState(String state);
    Address findByCountry(String country);
    List<Address> findAll();
    void delete(Address address);
    void deleteById(UUID id);
    void update(Address address);
}
