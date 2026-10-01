package com.longobuccodev.app_accommodation.core.domain;

import com.longobuccodev.app_accommodation.core.exception.InvalidEmployeeException;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class Employee {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int CPF_LENGTH = 11;
    private static final int MIN_PHONE_DIGITS = 10;
    private static final int MAX_PHONE_DIGITS = 11;
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 100;

    private UUID id;
    private String name;
    private String email;
    private String cpf;
    private String phone;
    private Address address;
    private LocalDate birthDate;
    private CostCenter costCenter;
    private Set<Project> projects;
    private Role role;

    public Employee(UUID id, String name, String email, String cpf, String phone, Address address,
                    LocalDate birthDate, CostCenter costCenter, Set<Project> projects, Role role) {
        setId(id);
        setName(name);
        setEmail(email);
        setCpf(cpf);
        setPhone(phone);
        setAddress(address);
        setBirthDate(birthDate);
        setCostCenter(costCenter);
        setProjects(projects);
        setRole(role);
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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = validateCpf(cpf);
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
        this.address = address;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = validateBirthDate(birthDate);
    }

    public CostCenter getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenter costCenter) {
        this.costCenter = validateCostCenter(costCenter);
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public void setProjects(Set<Project> projects) {
        this.projects = validateProjects(projects);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = validateRole(role);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Employee employee && Objects.equals(id, employee.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw InvalidEmployeeException.blankName();
        }
        String normalized = name.strip().replaceAll("\\s+", " ");
        if (normalized.length() < MIN_NAME_LENGTH) {
            throw InvalidEmployeeException.shortName(normalized);
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw InvalidEmployeeException.longName(normalized);
        }
        return normalized;
    }

    private static String validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.strip()).matches()) {
            throw InvalidEmployeeException.invalidEmail(email);
        }
        return email.strip().toLowerCase();
    }

    private static String validateCpf(String cpf) {
        if (cpf == null) {
            throw InvalidEmployeeException.invalidCpf(cpf);
        }
        String digits = cpf.replaceAll("[^0-9]", "");
        if (!isValidCpf(digits)) {
            throw InvalidEmployeeException.invalidCpf(cpf);
        }
        return digits;
    }

    private static String validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < MIN_PHONE_DIGITS || digits.length() > MAX_PHONE_DIGITS) {
            throw InvalidEmployeeException.invalidPhone(phone);
        }
        return digits;
    }

    private static LocalDate validateBirthDate(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw InvalidEmployeeException.invalidBirthDate(birthDate);
        }
        return birthDate;
    }

    private static CostCenter validateCostCenter(CostCenter costCenter) {
        if (costCenter == null) {
            throw InvalidEmployeeException.missingCostCenter();
        }
        return costCenter;
    }

    private static Set<Project> validateProjects(Set<Project> projects) {
        if (projects == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(projects);
    }

    private static Role validateRole(Role role) {
        if (role == null) {
            throw InvalidEmployeeException.missingRole();
        }
        return role;
    }

    private static boolean isValidCpf(String digits) {
        if (digits.length() != CPF_LENGTH || digits.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigitSum = 0;
        for (int index = 0; index < 9; index++) {
            firstDigitSum += Character.getNumericValue(digits.charAt(index)) * (10 - index);
        }
        if (checkDigit(firstDigitSum) != Character.getNumericValue(digits.charAt(9))) {
            return false;
        }

        int secondDigitSum = 0;
        for (int index = 0; index < 10; index++) {
            secondDigitSum += Character.getNumericValue(digits.charAt(index)) * (11 - index);
        }
        return checkDigit(secondDigitSum) == Character.getNumericValue(digits.charAt(10));
    }

    private static int checkDigit(int sum) {
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    public enum Role {
        MONTADOR,
        ENCARREGADO,
        SUPERVISOR,
        TEC_SEGURANCA,
        ALPINISTA
    }
}