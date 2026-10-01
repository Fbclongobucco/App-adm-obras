package com.longobuccodev.app_accommodation.core.domain;

import com.longobuccodev.app_accommodation.core.exception.InvalidClientException;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class Client {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MIN_PHONE_DIGITS = 10;
    private static final int MAX_PHONE_DIGITS = 11;

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Address address;
    private Set<Project> projects;

    public Client(UUID id, String name, String email, String phone, Address address, Set<Project> projects) {
        setId(id);
        setName(name);
        setEmail(email);
        setPhone(phone);
        setAddress(address);
        setProjects(projects);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateName(name);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = validateEmail(email);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = validatePhone(phone);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = validateAddress(address);
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public void setProjects(Set<Project> projects) {
        this.projects = validateProjects(projects);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Client client && Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw InvalidClientException.blankName();
        }
        String normalized = name.strip().replaceAll("\\s+", " ");
        if (normalized.length() < MIN_NAME_LENGTH) {
            throw InvalidClientException.shortName(normalized);
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw InvalidClientException.longName(normalized);
        }
        return normalized;
    }

    private static String validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.strip()).matches()) {
            throw InvalidClientException.invalidEmail(email);
        }
        return email.strip().toLowerCase();
    }

    private static String validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw InvalidClientException.invalidPhone(phone);
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < MIN_PHONE_DIGITS || digits.length() > MAX_PHONE_DIGITS) {
            throw InvalidClientException.invalidPhone(phone);
        }
        return digits;
    }

    private static Address validateAddress(Address address) {
        if (address == null) {
            throw InvalidClientException.missingAddress();
        }
        return address;
    }

    private static Set<Project> validateProjects(Set<Project> projects) {
        if (projects == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(projects);
    }
}