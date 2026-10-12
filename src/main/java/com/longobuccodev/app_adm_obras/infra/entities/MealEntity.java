package com.longobuccodev.app_adm_obras.infra.entities;

import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meal")
public class MealEntity extends AuditedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantEntity restaurant;

    @Column(name = "is_billed", nullable = false)
    private Boolean isBilled;

    // Copia da obra do restaurante, para a unicidade de cobertura (uk_meal_project_lunch/dinner) valer no banco
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Enumerated(EnumType.STRING)
    // VARCHAR em todos os bancos, como nas migracoes; sem isso o H2Dialect usa ENUM nativo
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(name = "lunch_quantity")
    private Integer lunchQuantity;

    @Column(name = "lunch_price", precision = 19, scale = 2)
    private BigDecimal lunchPrice;

    @Column(name = "dinner_quantity")
    private Integer dinnerQuantity;

    @Column(name = "dinner_price", precision = 19, scale = 2)
    private BigDecimal dinnerPrice;

    // Derivadas do tipo: TRUE quando a refeicao serve almoco/jantar, NULL quando nao serve.
    // UNIQUE (project_id, lunch_slot) e (project_id, dinner_slot) ignoram NULL no PostgreSQL e no H2.
    @Column(name = "lunch_slot")
    private Boolean lunchSlot;

    @Column(name = "dinner_slot")
    private Boolean dinnerSlot;

    // Ajustes por data: parte da refeicao, gravados e apagados com ela. Lidos em lote nas listagens.
    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<MealAdjustmentEntity> adjustments = new ArrayList<>();

    public static MealEntity create() {
        return new MealEntity();
    }

    protected MealEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RestaurantEntity getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(RestaurantEntity restaurant) {
        this.restaurant = restaurant;
    }

    public Boolean getIsBilled() {
        return isBilled;
    }

    public void setIsBilled(Boolean isBilled) {
        this.isBilled = isBilled;
    }


    public ProjectEntity getProject() {
        return project;
    }

    public void setProject(ProjectEntity project) {
        this.project = project;
    }

    public MealType getMealType() {
        return mealType;
    }

    public void setMealType(MealType mealType) {
        this.mealType = mealType;
    }

    public Integer getLunchQuantity() {
        return lunchQuantity;
    }

    public void setLunchQuantity(Integer lunchQuantity) {
        this.lunchQuantity = lunchQuantity;
    }

    public BigDecimal getLunchPrice() {
        return lunchPrice;
    }

    public void setLunchPrice(BigDecimal lunchPrice) {
        this.lunchPrice = lunchPrice;
    }

    public Integer getDinnerQuantity() {
        return dinnerQuantity;
    }

    public void setDinnerQuantity(Integer dinnerQuantity) {
        this.dinnerQuantity = dinnerQuantity;
    }

    public BigDecimal getDinnerPrice() {
        return dinnerPrice;
    }

    public void setDinnerPrice(BigDecimal dinnerPrice) {
        this.dinnerPrice = dinnerPrice;
    }

    public Boolean getLunchSlot() {
        return lunchSlot;
    }

    public void setLunchSlot(Boolean lunchSlot) {
        this.lunchSlot = lunchSlot;
    }

    public Boolean getDinnerSlot() {
        return dinnerSlot;
    }

    public void setDinnerSlot(Boolean dinnerSlot) {
        this.dinnerSlot = dinnerSlot;
    }

    public List<MealAdjustmentEntity> getAdjustments() {
        return adjustments;
    }
}
