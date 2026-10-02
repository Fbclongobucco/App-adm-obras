package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.exception.InvalidProjectException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class Project {

    private static final Pattern OS_PATTERN = Pattern.compile("^[A-Za-z0-9._/-]+$");

    private static final int MIN_OS_LENGTH = 3;
    private static final int MAX_OS_LENGTH = 30;
    private static final int MIN_DESCRIPTION_LENGTH = 3;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private UUID id;
    private String os;
    private String description;
    private CostCenter costCenter;
    private LocalDate startDate;
    private LocalDate endDate;
    private Client client;
    private final Set<Accommodation> accommodations = new LinkedHashSet<>();
    private final Set<Employee> employees = new LinkedHashSet<>();
    private Meal lunch;
    private Meal dinner;
    private BigDecimal totalPrice;
    private Boolean isCompleted;

    public Project(UUID id, String os, String description, CostCenter costCenter, LocalDate startDate,
                   LocalDate endDate, Client client, Boolean isCompleted) {
        setId(id);
        setOs(os);
        setDescription(description);
        setCostCenter(costCenter);
        setStartDate(startDate);
        setEndDate(endDate);
        setClient(client);
        setCompleted(isCompleted);
        refreshTotalPrice();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = validateOs(os);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = validateDescription(description);
    }

    public CostCenter getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenter costCenter) {
        this.costCenter = validateCostCenter(costCenter);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = validateStartDate(startDate);
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = validateEndDate(this.startDate, endDate);
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        Client validated = validateClient(client);
        if (this.client == validated) {
            return;
        }
        Client previous = this.client;
        this.client = validated;
        if (previous != null) {
            previous.removeProject(this);
        }
        validated.addProject(this);
    }

    public Set<Accommodation> getAccommodations() {
        return Collections.unmodifiableSet(accommodations);
    }

    public void addAccommodation(Accommodation accommodation) {
        if (accommodation == null) {
            throw InvalidProjectException.missingAccommodation();
        }
        accommodations.add(accommodation);
        if (accommodation.getProject() != this) {
            accommodation.setProject(this);
        }
        refreshTotalPrice();
    }

    public void removeAccommodation(Accommodation accommodation) {
        if (accommodations.remove(accommodation) && accommodation.getProject() == this) {
            accommodation.setProject(null);
        }
        refreshTotalPrice();
    }

    public Set<Employee> getEmployees() {
        return Collections.unmodifiableSet(employees);
    }

    public void addEmployee(Employee employee) {
        if (employee == null) {
            throw InvalidProjectException.missingEmployee();
        }
        if (employees.add(employee)) {
            employee.addProject(this);
        }
    }

    public void removeEmployee(Employee employee) {
        if (employees.remove(employee)) {
            employee.removeProject(this);
        }
    }

    public Meal getLunch() {
        return lunch;
    }

    public void setLunch(Meal lunch) {
        this.lunch = validateMeal(lunch, MealType.LUNCH);
        refreshTotalPrice();
    }

    public Meal getDinner() {
        return dinner;
    }

    public void setDinner(Meal dinner) {
        this.dinner = validateMeal(dinner, MealType.DINNER);
        refreshTotalPrice();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public boolean isCompleted() {
        return Boolean.TRUE.equals(isCompleted);
    }

    public void setCompleted(Boolean completed) {
        this.isCompleted = completed != null && completed;
    }

    public void addMeal(Meal meal) {
        if (meal == null) {
            throw InvalidProjectException.missingMeal();
        }
        MealType mealType = validateMealType(meal);
        if (mealType == MealType.LUNCH) {
            if (lunch != null) {
                throw InvalidProjectException.duplicatedMealType(mealType.name());
            }
            setLunch(meal);
        } else {
            if (dinner != null) {
                throw InvalidProjectException.duplicatedMealType(mealType.name());
            }
            setDinner(meal);
        }
    }

    public void removeMeal(MealType mealType) {
        if (mealType == MealType.LUNCH) {
            if (lunch == null) {
                throw InvalidProjectException.missingMealType(mealType.name());
            }
            setLunch(null);
        } else {
            if (dinner == null) {
                throw InvalidProjectException.missingMealType(mealType.name());
            }
            setDinner(null);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Project project && Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    void refreshTotalPrice() {
        this.totalPrice = calculateTotalPrice();
    }

    private BigDecimal calculateTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;
        if (lunch != null) {
            total = total.add(lunch.getTotalPrice());
        }
        if (dinner != null) {
            total = total.add(dinner.getTotalPrice());
        }
        for (Accommodation accommodation : accommodations) {
            total = total.add(accommodation.getTotalPrice());
        }
        return total;
    }

    private static String validateOs(String os) {
        if (os == null || os.isBlank()) {
            throw InvalidProjectException.blankOs();
        }
        String normalized = os.strip().toUpperCase();
        if (normalized.length() < MIN_OS_LENGTH
                || normalized.length() > MAX_OS_LENGTH
                || !OS_PATTERN.matcher(normalized).matches()) {
            throw InvalidProjectException.invalidOs(normalized);
        }
        return normalized;
    }

    private static String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String normalized = description.strip().replaceAll("\\s+", " ");
        if (normalized.length() < MIN_DESCRIPTION_LENGTH || normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw InvalidProjectException.invalidDescription(normalized);
        }
        return normalized;
    }

    private static CostCenter validateCostCenter(CostCenter costCenter) {
        if (costCenter == null) {
            throw InvalidProjectException.missingCostCenter();
        }
        return costCenter;
    }

    private static LocalDate validateStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw InvalidProjectException.missingStartDate();
        }
        return startDate;
    }

    private static LocalDate validateEndDate(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw InvalidProjectException.invalidEndDate(startDate, endDate);
        }
        return endDate;
    }

    private static Client validateClient(Client client) {
        if (client == null) {
            throw InvalidProjectException.missingClient();
        }
        return client;
    }

    private static Meal validateMeal(Meal meal, MealType expectedType) {
        if (meal == null) {
            return null;
        }
        MealType mealType = validateMealType(meal);
        if (mealType != expectedType) {
            throw InvalidProjectException.mealTypeMismatch(expectedType.name(), mealType.name());
        }
        return meal;
    }

    private static MealType validateMealType(Meal meal) {
        MealType mealType = meal.getMealType();
        if (mealType == null) {
            throw InvalidProjectException.missingMeal();
        }
        return mealType;
    }
}