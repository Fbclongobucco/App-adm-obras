package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidAccommodationException;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Accommodation {

    private static final int MIN_HOST_NAME_LENGTH = 3;
    private static final int MAX_HOST_NAME_LENGTH = 100;
    private static final int MIN_PHONE_DIGITS = 10;
    private static final int MAX_PHONE_DIGITS = 11;
    private static final int MIN_CAPACITY = 1;
    private static final int MAX_CAPACITY = 1000;
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;

    private UUID id;
    private String hostName;
    private String hostPhone;
    private Address address;
    private Integer capacity;
    private Integer days;
    private Boolean isContract;
    private Project project;
    private final Set<Employee> employees;
    private BigDecimal totalPrice;

    public Accommodation(UUID id, String hostName, String hostPhone, Address address, Integer capacity,
                         Integer days, Boolean isContract, Project project, Set<Employee> employees,
                         BigDecimal totalPrice) {
        setId(id);
        setHostName(hostName);
        setHostPhone(hostPhone);
        setAddress(address);
        setCapacity(capacity);
        setDays(days);
        setContract(isContract);
        this.employees = validateEmployees(employees);
        setTotalPrice(totalPrice);
        setProject(project);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = validateHostName(hostName);
    }

    public String getHostPhone() {
        return hostPhone;
    }

    public void setHostPhone(String hostPhone) {
        this.hostPhone = validateHostPhone(hostPhone);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = validateAddress(address);
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = validateCapacity(capacity);
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = validateDays(days);
    }

    public Boolean getIsContract() {
        return isContract;
    }

    public boolean isContract() {
        return Boolean.TRUE.equals(isContract);
    }

    public void setContract(Boolean contract) {
        this.isContract = contract != null && contract;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        if (this.project == project) {
            return;
        }
        Project previous = this.project;
        this.project = project;
        if (previous != null) {
            previous.removeAccommodation(this);
        }
        if (project != null) {
            project.addAccommodation(this);
        }
    }

    public Set<Employee> getEmployees() {
        return employees;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = validateTotalPrice(totalPrice);
        if (project != null) {
            project.refreshTotalPrice();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Accommodation accommodation && Objects.equals(id, accommodation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static String validateHostName(String hostName) {
        if (hostName == null || hostName.isBlank()) {
            throw InvalidAccommodationException.blankHostName();
        }
        String normalized = hostName.strip().replaceAll("\\s+", " ");
        if (normalized.length() < MIN_HOST_NAME_LENGTH || normalized.length() > MAX_HOST_NAME_LENGTH) {
            throw InvalidAccommodationException.invalidHostName(normalized);
        }
        return normalized;
    }

    private static String validateHostPhone(String hostPhone) {
        if (hostPhone == null || hostPhone.isBlank()) {
            throw InvalidAccommodationException.invalidHostPhone(hostPhone);
        }
        String digits = hostPhone.replaceAll("[^0-9]", "");
        if (digits.length() < MIN_PHONE_DIGITS || digits.length() > MAX_PHONE_DIGITS) {
            throw InvalidAccommodationException.invalidHostPhone(hostPhone);
        }
        return digits;
    }

    private static Address validateAddress(Address address) {
        if (address == null) {
            throw InvalidAccommodationException.missingAddress();
        }
        return address;
    }

    private static Integer validateCapacity(Integer capacity) {
        if (capacity == null || capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw InvalidAccommodationException.invalidCapacity(capacity);
        }
        return capacity;
    }

    private static Integer validateDays(Integer days) {
        if (days == null || days < MIN_DAYS || days > MAX_DAYS) {
            throw InvalidAccommodationException.invalidDays(days);
        }
        return days;
    }

    private static BigDecimal validateTotalPrice(BigDecimal totalPrice) {
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw InvalidAccommodationException.invalidTotalPrice(totalPrice);
        }
        return totalPrice;
    }

    private static Set<Employee> validateEmployees(Set<Employee> employees) {
        if (employees == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(employees);
    }
}