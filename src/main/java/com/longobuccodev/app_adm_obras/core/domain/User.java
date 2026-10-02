package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidUserException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class User {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 100;

    private UUID id;
    private String name;
    private String email;
    private Boolean isActive;
    private final Set<Role> roles = EnumSet.noneOf(Role.class);

    public User(UUID id, String name, String email, Boolean isActive) {
        setId(id);
        setName(name);
        setEmail(email);
        setActive(isActive);
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

    public Boolean getIsActive() {
        return isActive;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public void setActive(Boolean active) {
        this.isActive = active != null && active;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void addRole(Role role) {
        if (role == null) {
            throw InvalidUserException.missingRole();
        }
        roles.add(role);
    }

    public void removeRole(Role role) {
        roles.remove(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof User user && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw InvalidUserException.blankName();
        }
        String normalized = name.strip().replaceAll("\\s+", " ");
        if (normalized.length() < MIN_NAME_LENGTH) {
            throw InvalidUserException.shortName(normalized);
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw InvalidUserException.longName(normalized);
        }
        return normalized;
    }

    private static String validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.strip()).matches()) {
            throw InvalidUserException.invalidEmail(email);
        }
        return email.strip().toLowerCase();
    }

    public enum Role {
        ADMIN,
        OPERADOR
    }
}
