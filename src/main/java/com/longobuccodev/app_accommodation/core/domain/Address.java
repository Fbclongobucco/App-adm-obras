package com.longobuccodev.app_accommodation.core.domain;

import com.longobuccodev.app_accommodation.core.exception.InvalidAddressException;

import java.util.Objects;
import java.util.UUID;

public class Address {

    private static final int MIN_STREET_LENGTH = 2;
    private static final int MAX_STREET_LENGTH = 100;
    private static final int MAX_NUMBER_LENGTH = 10;
    private static final int MIN_CITY_LENGTH = 2;
    private static final int MAX_CITY_LENGTH = 60;
    private static final int MIN_NEIGHBORHOOD_LENGTH = 2;
    private static final int MAX_NEIGHBORHOOD_LENGTH = 60;
    private static final int STATE_LENGTH = 2;
    private static final int MAX_COUNTRY_LENGTH = 40;
    private static final int ZIP_CODE_LENGTH = 8;

    private UUID id;
    private String street;
    private String number;
    private String city;
    private String state;
    private String country;
    private String neighborhood;
    private String zipCode;

    public Address(UUID id, String street, String number, String city, String state, String country,
                   String neighborhood, String zipCode) {
        setId(id);
        setStreet(street);
        setNumber(number);
        setCity(city);
        setState(state);
        setCountry(country);
        setNeighborhood(neighborhood);
        setZipCode(zipCode);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = validateStreet(street);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = validateNumber(number);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = validateCity(city);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = validateState(state);
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = validateCountry(country);
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = validateNeighborhood(neighborhood);
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = validateZipCode(zipCode);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Address address && Objects.equals(id, address.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static String validateStreet(String street) {
        if (street == null || street.isBlank()) {
            throw InvalidAddressException.missingStreet();
        }
        String normalized = normalize(street);
        if (normalized.length() < MIN_STREET_LENGTH || normalized.length() > MAX_STREET_LENGTH) {
            throw InvalidAddressException.missingStreet();
        }
        return normalized;
    }

    private static String validateNumber(String number) {
        if (number == null || number.isBlank()) {
            throw InvalidAddressException.missingNumber();
        }
        String normalized = normalize(number);
        if (normalized.length() > MAX_NUMBER_LENGTH) {
            throw InvalidAddressException.missingNumber();
        }
        return normalized;
    }

    private static String validateCity(String city) {
        if (city == null || city.isBlank()) {
            throw InvalidAddressException.missingCity();
        }
        String normalized = normalize(city);
        if (normalized.length() < MIN_CITY_LENGTH || normalized.length() > MAX_CITY_LENGTH) {
            throw InvalidAddressException.missingCity();
        }
        return normalized;
    }

    private static String validateState(String state) {
        if (state == null || state.isBlank()) {
            throw InvalidAddressException.missingState();
        }
        String normalized = state.strip().toUpperCase();
        if (normalized.length() != STATE_LENGTH || !normalized.chars().allMatch(Character::isLetter)) {
            throw InvalidAddressException.invalidState(normalized);
        }
        return normalized;
    }

    private static String validateCountry(String country) {
        if (country == null || country.isBlank()) {
            throw InvalidAddressException.missingCountry();
        }
        String normalized = normalize(country);
        if (normalized.length() < 2 || normalized.length() > MAX_COUNTRY_LENGTH) {
            throw InvalidAddressException.missingCountry();
        }
        return normalized;
    }

    private static String validateNeighborhood(String neighborhood) {
        if (neighborhood == null || neighborhood.isBlank()) {
            throw InvalidAddressException.missingNeighborhood();
        }
        String normalized = normalize(neighborhood);
        if (normalized.length() < MIN_NEIGHBORHOOD_LENGTH || normalized.length() > MAX_NEIGHBORHOOD_LENGTH) {
            throw InvalidAddressException.missingNeighborhood();
        }
        return normalized;
    }

    private static String validateZipCode(String zipCode) {
        if (zipCode == null || zipCode.isBlank()) {
            throw InvalidAddressException.invalidZipCode(zipCode);
        }
        String digits = zipCode.replaceAll("[^0-9]", "");
        if (digits.length() != ZIP_CODE_LENGTH) {
            throw InvalidAddressException.invalidZipCode(zipCode);
        }
        return digits;
    }

    private static String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }
}