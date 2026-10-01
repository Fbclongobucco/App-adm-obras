package com.longobuccodev.app_accommodation.core.domain;

import com.longobuccodev.app_accommodation.core.exception.InvalidMealException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Meal {

    private static final int MIN_RESTAURANT_NAME_LENGTH = 2;
    private static final int MAX_RESTAURANT_NAME_LENGTH = 100;
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 10000;

    private UUID id;
    private String restaurantName;
    private BigDecimal price;
    private Boolean isBilled;
    private Address address;
    private Project project;
    private MealType mealType;
    private Integer quantity;
    private LocalDate date;
    private BigDecimal totalPrice;

    public Meal(UUID id, String restaurantName, BigDecimal price, Boolean isBilled, Address address,
                Project project, MealType mealType, Integer quantity, LocalDate date) {
        setId(id);
        setRestaurantName(restaurantName);
        setPrice(price);
        setBilled(isBilled);
        setAddress(address);
        setProject(project);
        setMealType(mealType);
        setQuantity(quantity);
        setDate(date);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = validateRestaurantName(restaurantName);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = validatePrice(price);
        this.totalPrice = calculateTotalPrice();
    }

    public Boolean getBilled() {
        return isBilled;
    }

    public boolean isBilled() {
        return Boolean.TRUE.equals(isBilled);
    }

    public void setBilled(Boolean billed) {
        this.isBilled = validateBilled(billed);
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
        this.project = project;
    }

    public MealType getMealType() {
        return mealType;
    }

    public void setMealType(MealType mealType) {
        this.mealType = validateMealType(mealType);
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = validateQuantity(quantity);
        this.totalPrice = calculateTotalPrice();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = validateDate(date);
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Meal meal && Objects.equals(id, meal.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private BigDecimal calculateTotalPrice() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    private static String validateRestaurantName(String restaurantName) {
        if (restaurantName == null || restaurantName.isBlank()) {
            throw InvalidMealException.blankRestaurantName();
        }
        String normalized = restaurantName.strip().replaceAll("\\s+", " ");
        if (normalized.length() < MIN_RESTAURANT_NAME_LENGTH
                || normalized.length() > MAX_RESTAURANT_NAME_LENGTH) {
            throw InvalidMealException.invalidRestaurantName(normalized);
        }
        return normalized;
    }

    private static BigDecimal validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidMealException.invalidPrice(price);
        }
        return price;
    }

    private static Boolean validateBilled(Boolean billed) {
        return billed != null && billed;
    }

    private static MealType validateMealType(MealType mealType) {
        if (mealType == null) {
            throw InvalidMealException.missingMealType();
        }
        return mealType;
    }

    private static Integer validateQuantity(Integer quantity) {
        if (quantity == null || quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw InvalidMealException.invalidQuantity(quantity);
        }
        return quantity;
    }

    private static LocalDate validateDate(LocalDate date) {
        if (date == null) {
            throw InvalidMealException.missingDate();
        }
        if (date.isAfter(LocalDate.now())) {
            throw InvalidMealException.futureDate(date);
        }
        return date;
    }

    public enum MealType {
        LUNCH,
        DINNER
    }
}