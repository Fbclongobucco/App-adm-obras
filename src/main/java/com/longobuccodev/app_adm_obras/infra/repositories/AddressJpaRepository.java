package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, UUID> {

    Optional<AddressEntity> findFirstByZipCode(String zipCode);

    Optional<AddressEntity> findFirstByStreet(String street);

    Optional<AddressEntity> findFirstByNumber(String number);

    Optional<AddressEntity> findFirstByNeighborhood(String neighborhood);

    Optional<AddressEntity> findFirstByCity(String city);

    Optional<AddressEntity> findFirstByState(String state);

    Optional<AddressEntity> findFirstByCountry(String country);
}
