package com.longobuccodev.app_adm_obras.infra.entities;

import com.longobuccodev.app_adm_obras.core.domain.MealAdjustment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "meal_adjustment")
public class MealAdjustmentEntity extends AuditedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_id", nullable = false)
    private MealEntity meal;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    // VARCHAR em todos os bancos, como na migracao; sem isso o H2Dialect usa ENUM nativo
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 20)
    private MealAdjustment.Type type;

    @Column(name = "lunch_quantity")
    private Integer lunchQuantity;

    @Column(name = "dinner_quantity")
    private Integer dinnerQuantity;

    @Column(name = "note", length = 100)
    private String note;

    public static MealAdjustmentEntity create() {
        return new MealAdjustmentEntity();
    }

    protected MealAdjustmentEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MealEntity getMeal() {
        return meal;
    }

    public void setMeal(MealEntity meal) {
        this.meal = meal;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public MealAdjustment.Type getType() {
        return type;
    }

    public void setType(MealAdjustment.Type type) {
        this.type = type;
    }

    public Integer getLunchQuantity() {
        return lunchQuantity;
    }

    public void setLunchQuantity(Integer lunchQuantity) {
        this.lunchQuantity = lunchQuantity;
    }

    public Integer getDinnerQuantity() {
        return dinnerQuantity;
    }

    public void setDinnerQuantity(Integer dinnerQuantity) {
        this.dinnerQuantity = dinnerQuantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
