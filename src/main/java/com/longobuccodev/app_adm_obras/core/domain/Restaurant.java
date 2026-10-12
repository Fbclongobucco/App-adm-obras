package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidRestaurantException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Restaurante que atende uma obra. Todo restaurante pertence a exatamente uma obra. */
public class Restaurant extends Audited {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MIN_PHONE_DIGITS = 10;
    private static final int MAX_PHONE_DIGITS = 11;

    private UUID id;
    private String name;
    private String phone;
    private Address address;
    private Project project;
    private BigDecimal deliveryFee;
    private BigDecimal sodaPrice;

    /** Restaurante sem taxa de entrega e sem refrigerante. */
    public Restaurant(UUID id, String name, String phone, Address address, Project project) {
        this(id, name, phone, address, project, null);
    }

    /** Restaurante sem refrigerante. */
    public Restaurant(UUID id, String name, String phone, Address address, Project project, BigDecimal deliveryFee) {
        this(id, name, phone, address, project, deliveryFee, null);
    }

    public Restaurant(UUID id, String name, String phone, Address address, Project project, BigDecimal deliveryFee,
                      BigDecimal sodaPrice) {
        setId(id);
        setName(name);
        setPhone(phone);
        setAddress(address);
        setProject(project);
        setDeliveryFee(deliveryFee);
        setSodaPrice(sodaPrice);
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        if (project == null) {
            throw InvalidRestaurantException.missingProject();
        }
        this.project = project;
    }

    /** Taxa cobrada por entrega; {@code null} quando o restaurante nao cobra entrega. */
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        if (deliveryFee != null && deliveryFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidRestaurantException.invalidDeliveryFee(deliveryFee);
        }
        this.deliveryFee = deliveryFee;
    }

    /**
     * Preco unitario do refrigerante, 1 por entrega em todas as refeicoes do restaurante;
     * {@code null} quando o restaurante nao fornece refrigerante.
     */
    public BigDecimal getSodaPrice() {
        return sodaPrice;
    }

    public void setSodaPrice(BigDecimal sodaPrice) {
        if (sodaPrice != null && sodaPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidRestaurantException.invalidSodaPrice(sodaPrice);
        }
        this.sodaPrice = sodaPrice;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Restaurant restaurant && Objects.equals(id, restaurant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw InvalidRestaurantException.invalidName(name);
        }
        String normalized = name.strip().replaceAll("\\s+", " ");
        if (normalized.length() < MIN_NAME_LENGTH || normalized.length() > MAX_NAME_LENGTH) {
            throw InvalidRestaurantException.invalidName(normalized);
        }
        return normalized;
    }

    private static String validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < MIN_PHONE_DIGITS || digits.length() > MAX_PHONE_DIGITS) {
            throw InvalidRestaurantException.invalidPhone(phone);
        }
        return digits;
    }
}
