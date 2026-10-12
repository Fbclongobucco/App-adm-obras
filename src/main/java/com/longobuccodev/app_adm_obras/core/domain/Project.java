package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidProjectException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class Project extends Audited {

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
    private final Set<Restaurant> restaurants = new LinkedHashSet<>();
    private final Set<Employee> employees = new LinkedHashSet<>();
    private final List<Meal> meals = new ArrayList<>();
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
    }

    public void removeAccommodation(Accommodation accommodation) {
        if (accommodations.remove(accommodation) && accommodation.getProject() == this) {
            accommodation.setProject(null);
        }
    }

    /** Restaurantes que atendem a obra. Leitura apenas: o vinculo e definido pelo restaurante. */
    public Set<Restaurant> getRestaurants() {
        return Collections.unmodifiableSet(restaurants);
    }

    public void addRestaurant(Restaurant restaurant) {
        if (restaurant != null) {
            restaurants.add(restaurant);
        }
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

    /**
     * Refeicoes da obra, ordenadas por tipo. Leitura apenas: o vinculo e definido pelo
     * restaurante da refeicao.
     */
    public List<Meal> getMeals() {
        List<Meal> sorted = new ArrayList<>(meals);
        sorted.sort(Comparator.comparing(Meal::getMealType));
        return Collections.unmodifiableList(sorted);
    }

    /** Dias de segunda a sexta de startDate a endDate, inclusive, sem feriados; null sem termino. */
    public Integer getWorkingDays() {
        if (startDate == null || endDate == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long workingDays = (days / 7) * 5;
        DayOfWeek day = startDate.getDayOfWeek();
        for (long i = 0; i < days % 7; i++) {
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            day = day.plus(1);
        }
        return Math.toIntExact(workingDays);
    }

    /** Total estimado das refeicoes (sem termino conta zero) mais o total das hospedagens. */
    public BigDecimal getTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (Meal meal : meals) {
            BigDecimal mealTotal = meal.getTotalPrice();
            if (mealTotal != null) {
                total = total.add(mealTotal);
            }
        }
        for (Accommodation accommodation : accommodations) {
            total = total.add(accommodation.getTotalPrice());
        }
        return total;
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

    /**
     * Inclui a refeicao e liga a ela esta obra. Rejeita refeicao que sirva almoco ou jantar ja
     * servido por outra refeicao da obra.
     */
    public void addMeal(Meal meal) {
        if (meal == null || meal.getMealType() == null) {
            throw InvalidProjectException.missingMeal();
        }
        if (meals.stream().anyMatch(meal::overlaps)) {
            throw InvalidProjectException.duplicatedMealType(meal.getMealType().name());
        }
        meals.remove(meal);
        meals.add(meal);
        meal.setProject(this);
    }

    public void removeMeal(Meal meal) {
        meals.remove(meal);
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
}
