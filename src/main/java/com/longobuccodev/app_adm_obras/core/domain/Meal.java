package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidMealAdjustmentException;
import com.longobuccodev.app_adm_obras.core.exception.InvalidMealException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Alimentacao contratada de uma obra junto a um restaurante, sem data: refeicoes servidas e,
 * para cada uma, quantidade e preco por dia. Cada refeicao servida e uma entrega por dia, cobrada
 * pela taxa do restaurante para aquela refeicao (almoco ou jantar), com 1 refrigerante ao preco do
 * restaurante quando ele fornece. Os custos sao calculados a partir da obra e do restaurante atuais.
 * Ajustes por data (dia extra, dia alterado e sem fornecimento) entram no total estimado quando estao no
 * periodo atual da obra.
 */
public class Meal extends Audited {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 10000;
    private static final int MAX_DELIVERIES = 100;

    private UUID id;
    private Restaurant restaurant;
    private Boolean isBilled;
    private Project project;
    private MealType mealType;
    private Integer lunchQuantity;
    private BigDecimal lunchPrice;
    private Integer dinnerQuantity;
    private BigDecimal dinnerPrice;
    private final List<MealAdjustment> adjustments = new ArrayList<>();

    /** A obra da refeicao e a obra do restaurante. */
    public Meal(UUID id, Restaurant restaurant, MealType mealType, Boolean isBilled,
                Integer lunchQuantity, BigDecimal lunchPrice, Integer dinnerQuantity, BigDecimal dinnerPrice) {
        setId(id);
        setRestaurant(restaurant);
        setBilled(isBilled);
        configure(mealType, lunchQuantity, lunchPrice, dinnerQuantity, dinnerPrice);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            throw InvalidMealException.missingRestaurant();
        }
        this.restaurant = restaurant;
        this.project = restaurant.getProject();
    }

    public Boolean getBilled() {
        return isBilled;
    }

    public boolean isBilled() {
        return Boolean.TRUE.equals(isBilled);
    }

    public void setBilled(Boolean billed) {
        this.isBilled = billed != null && billed;
    }

    public Project getProject() {
        return project;
    }

    /** Usado pela obra ao incluir a refeicao, para os custos refletirem as datas dessa instancia. */
    public void setProject(Project project) {
        this.project = project;
    }

    public MealType getMealType() {
        return mealType;
    }

    public Integer getLunchQuantity() {
        return lunchQuantity;
    }

    public BigDecimal getLunchPrice() {
        return lunchPrice;
    }

    public Integer getDinnerQuantity() {
        return dinnerQuantity;
    }

    public BigDecimal getDinnerPrice() {
        return dinnerPrice;
    }

    /**
     * Preco unitario do refrigerante do restaurante, 1 por entrega; {@code null} quando o restaurante nao
     * fornece refrigerante. Lido do restaurante a cada chamada.
     */
    public BigDecimal getSodaPrice() {
        return restaurant.getSodaPrice();
    }

    /**
     * Define o tipo e, para cada refeicao servida por ele, a quantidade por dia e o preco unitario.
     * Campos de refeicao nao servida sao descartados. Nada muda se a validacao falhar.
     */
    public void configure(MealType mealType, Integer lunchQuantity, BigDecimal lunchPrice,
                          Integer dinnerQuantity, BigDecimal dinnerPrice) {
        if (mealType == null) {
            throw InvalidMealException.missingMealType();
        }
        Integer newLunchQuantity = null;
        BigDecimal newLunchPrice = null;
        if (mealType.servesLunch()) {
            newLunchQuantity = validateQuantity(lunchQuantity, InvalidMealException::invalidLunchQuantity);
            newLunchPrice = validatePrice(lunchPrice, InvalidMealException::invalidLunchPrice);
        }
        Integer newDinnerQuantity = null;
        BigDecimal newDinnerPrice = null;
        if (mealType.servesDinner()) {
            newDinnerQuantity = validateQuantity(dinnerQuantity, InvalidMealException::invalidDinnerQuantity);
            newDinnerPrice = validatePrice(dinnerPrice, InvalidMealException::invalidDinnerPrice);
        }
        this.mealType = mealType;
        this.lunchQuantity = newLunchQuantity;
        this.lunchPrice = newLunchPrice;
        this.dinnerQuantity = newDinnerQuantity;
        this.dinnerPrice = newDinnerPrice;
    }

    /** Entregas por dia: uma por refeicao servida. */
    public int getDeliveriesPerDay() {
        return mealType.deliveriesPerDay();
    }

    /** Marmitas: soma de quantidade x preco de cada refeicao servida. */
    public BigDecimal getMealsDailyPrice() {
        BigDecimal daily = BigDecimal.ZERO;
        if (mealType.servesLunch()) {
            daily = daily.add(lunchPrice.multiply(BigDecimal.valueOf(lunchQuantity)));
        }
        if (mealType.servesDinner()) {
            daily = daily.add(dinnerPrice.multiply(BigDecimal.valueOf(dinnerQuantity)));
        }
        return daily;
    }

    /**
     * Uma entrega por refeicao servida, cada uma pela taxa do restaurante para aquela refeicao; taxa vazia
     * conta zero.
     */
    public BigDecimal getDeliveryDailyPrice() {
        BigDecimal daily = BigDecimal.ZERO;
        if (mealType.servesLunch() && restaurant.getLunchDeliveryFee() != null) {
            daily = daily.add(restaurant.getLunchDeliveryFee());
        }
        if (mealType.servesDinner() && restaurant.getDinnerDeliveryFee() != null) {
            daily = daily.add(restaurant.getDinnerDeliveryFee());
        }
        return daily;
    }

    /** Entregas por dia x preco do refrigerante do restaurante; zero quando o restaurante nao fornece. */
    public BigDecimal getSodaDailyPrice() {
        return perDelivery(getSodaPrice());
    }

    /** Custo por dia: marmitas, entrega e refrigerante. */
    public BigDecimal getDailyPrice() {
        return getMealsDailyPrice().add(getDeliveryDailyPrice()).add(getSodaDailyPrice());
    }

    /** Quantidades e valor da refeicao nos dias uteis fixos do periodo. */
    public MealProjection projection(BillingPeriod period) {
        int days = period.getWorkingDays();
        int deliveries = getDeliveriesPerDay() * days;
        return new MealProjection(
                period,
                days,
                mealType.servesLunch() ? lunchQuantity * days : 0,
                mealType.servesDinner() ? dinnerQuantity * days : 0,
                deliveries,
                getSodaPrice() == null ? 0 : deliveries,
                getDailyPrice().multiply(BigDecimal.valueOf(days))
        );
    }

    /** Projecao em cada periodo de fechamento, na ordem de {@link BillingPeriod}. */
    public List<MealProjection> getProjections() {
        return Arrays.stream(BillingPeriod.values()).map(this::projection).toList();
    }

    /** Dias uteis da obra; {@code null} se a obra nao tem data de termino. */
    public Integer getWorkingDays() {
        return project == null ? null : project.getWorkingDays();
    }

    /** Ajustes sem fornecimento dentro do periodo atual da obra. */
    public int getNoServiceDays() {
        return (int) adjustmentsInPeriod(MealAdjustment.Type.NO_SERVICE).count();
    }

    /** Dias extras dentro do periodo atual da obra. */
    public int getExtraDays() {
        return (int) adjustmentsInPeriod(MealAdjustment.Type.EXTRA_DAY).count();
    }

    /** Dias alterados dentro do periodo atual da obra. */
    public int getChangedDays() {
        return (int) adjustmentsInPeriod(MealAdjustment.Type.CHANGED).count();
    }

    /** Soma do valor dos dias extras dentro do periodo atual da obra. */
    public BigDecimal getExtraDaysPrice() {
        return adjustmentsInPeriod(MealAdjustment.Type.EXTRA_DAY)
                .map(adjustment -> adjustment.price(this))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Dias uteis menos os dias sem fornecimento; {@code null} se a obra nao tem data de termino. */
    public Integer getServiceDays() {
        Integer workingDays = getWorkingDays();
        return workingDays == null ? null : workingDays - getNoServiceDays();
    }

    /**
     * Soma do valor de cada dia do periodo da obra ({@link #dayClosing}), o mesmo calculo do fechamento
     * semanal; {@code null} se a obra nao tem data de termino.
     */
    public BigDecimal getTotalPrice() {
        if (project == null || project.getStartDate() == null || project.getEndDate() == null) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate date = project.getStartDate(); !date.isAfter(project.getEndDate()); date = date.plusDays(1)) {
            total = total.add(dayClosing(date).price());
        }
        return total;
    }

    /**
     * Fechamento do dia: fora do periodo da obra; ajuste na data (sem fornecimento zerado, dia extra e dia
     * alterado com os numeros do ajuste); sabado ou domingo sem ajuste; senao o fornecimento diario contratado.
     */
    public DayClosing dayClosing(LocalDate date) {
        if (date == null || !isWithinProject(date)) {
            return DayClosing.empty(date, id, DayClosing.Status.OUT_OF_PERIOD);
        }
        Optional<MealAdjustment> adjustment = findAdjustment(date);
        if (adjustment.isPresent()) {
            MealAdjustment found = adjustment.get();
            DayClosing.Status status = switch (found.getType()) {
                case NO_SERVICE -> DayClosing.Status.NO_SERVICE;
                case CHANGED -> DayClosing.Status.CHANGED;
                case EXTRA_DAY -> DayClosing.Status.EXTRA_DAY;
            };
            return new DayClosing(date, id, status, false, found.lunchMeals(this), found.dinnerMeals(this),
                    found.lunchDeliveries(this), found.dinnerDeliveries(this), found.sodas(this), found.price(this),
                    List.of(found.getId()), found.getNote(), List.of());
        }
        if (isWeekend(date)) {
            return DayClosing.empty(date, id, DayClosing.Status.OFF);
        }
        int lunchDeliveries = mealType.servesLunch() ? 1 : 0;
        int dinnerDeliveries = mealType.servesDinner() ? 1 : 0;
        return new DayClosing(date, id, DayClosing.Status.SERVICE, false,
                mealType.servesLunch() ? lunchQuantity : 0,
                mealType.servesDinner() ? dinnerQuantity : 0,
                lunchDeliveries, dinnerDeliveries, getSodaPrice() == null ? 0 : lunchDeliveries + dinnerDeliveries,
                getDailyPrice(), List.of(), null, List.of());
    }

    /**
     * Numeros do dia iguais ao fornecimento contratado: mesmas quantidades, 1 entrega por refeicao servida e,
     * com refrigerante, 1 por entrega. Usado pelo restaurante para nao gravar dia alterado sem efeito.
     */
    public boolean isDefaultDay(MealAdjustment adjustment) {
        int lunchDeliveries = mealType.servesLunch() ? 1 : 0;
        int dinnerDeliveries = mealType.servesDinner() ? 1 : 0;
        return adjustment.lunchMeals(this) == (mealType.servesLunch() ? lunchQuantity : 0)
                && adjustment.dinnerMeals(this) == (mealType.servesDinner() ? dinnerQuantity : 0)
                && adjustment.lunchDeliveries(this) == lunchDeliveries
                && adjustment.dinnerDeliveries(this) == dinnerDeliveries
                && adjustment.sodas(this) == (getSodaPrice() == null ? 0 : lunchDeliveries + dinnerDeliveries);
    }

    /** Ajustes por data, ordenados por data. Leitura apenas: inclusao por {@link #addAdjustment}. */
    public List<MealAdjustment> getAdjustments() {
        List<MealAdjustment> sorted = new ArrayList<>(adjustments);
        sorted.sort(Comparator.comparing(MealAdjustment::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(adjustment -> adjustment.getId().toString()));
        return Collections.unmodifiableList(sorted);
    }

    public Optional<MealAdjustment> findAdjustment(UUID adjustmentId) {
        return adjustments.stream().filter(adjustment -> adjustment.getId().equals(adjustmentId)).findFirst();
    }

    /** Ajuste da data, se houver (no maximo um por data). */
    public Optional<MealAdjustment> findAdjustment(LocalDate date) {
        return adjustments.stream().filter(adjustment -> date.equals(adjustment.getDate())).findFirst();
    }

    /**
     * Inclui o ajuste depois de validar (ver {@link #accept}); data repetida e conflito. Nada muda se a
     * validacao falhar.
     *
     * @return o ajuste incluido
     */
    public MealAdjustment addAdjustment(MealAdjustment adjustment) {
        if (adjustment == null) {
            throw InvalidMealAdjustmentException.missingType();
        }
        LocalDate date = adjustment.getDate();
        requireInProject(date);
        if (adjustments.stream().anyMatch(existing -> date.equals(existing.getDate())
                && !existing.getId().equals(adjustment.getId()))) {
            throw InvalidMealAdjustmentException.duplicateDate(date);
        }
        MealAdjustment accepted = accept(adjustment);
        adjustments.removeIf(existing -> existing.getId().equals(accepted.getId()));
        adjustments.add(accepted);
        return accepted;
    }

    /**
     * Grava o ajuste da data: substitui o existente, mantendo o id e a auditoria dele, ou inclui. Mesmas regras
     * de {@link #addAdjustment}, sem o conflito de data. Nada muda se a validacao falhar.
     *
     * @return o ajuste gravado (com a auditoria do anterior, quando substituiu)
     */
    public MealAdjustment putAdjustment(MealAdjustment adjustment) {
        if (adjustment == null) {
            throw InvalidMealAdjustmentException.missingType();
        }
        requireInProject(adjustment.getDate());
        Optional<MealAdjustment> existing = findAdjustment(adjustment.getDate());
        MealAdjustment candidate = existing.map(previous -> {
            MealAdjustment replacement = adjustment.withId(previous.getId());
            replacement.restoreAudit(previous.getAudit());
            return replacement;
        }).orElse(adjustment);
        MealAdjustment accepted = accept(candidate);
        existing.ifPresent(previous -> adjustments.remove(previous));
        adjustments.add(accepted);
        return accepted;
    }

    /** @return se havia ajuste com esse id */
    public boolean removeAdjustment(UUID adjustmentId) {
        return adjustments.removeIf(adjustment -> adjustment.getId().equals(adjustmentId));
    }

    /** Reidrata um ajuste ja gravado, sem regras (leitura do banco e copia para a refeicao alterada). */
    public void restoreAdjustment(MealAdjustment adjustment) {
        if (adjustment != null) {
            adjustments.removeIf(existing -> existing.getId().equals(adjustment.getId()));
            adjustments.add(adjustment);
        }
    }

    /** Data do ajuste dentro do periodo atual da obra (a partir do inicio; ate o termino, se houver). */
    public boolean isInPeriod(MealAdjustment adjustment) {
        return adjustment != null && adjustment.getDate() != null && isWithinProject(adjustment.getDate());
    }

    /** Outra refeicao (id diferente) que serve almoco ou jantar tambem servido por esta. */
    public boolean overlaps(Meal other) {
        if (other == null || Objects.equals(id, other.id)) {
            return false;
        }
        return mealType.overlaps(other.mealType);
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

    private Stream<MealAdjustment> adjustmentsInPeriod(MealAdjustment.Type type) {
        return adjustments.stream().filter(adjustment -> adjustment.getType() == type && isInPeriod(adjustment));
    }

    private boolean isWithinProject(LocalDate date) {
        if (project == null || project.getStartDate() == null || date.isBefore(project.getStartDate())) {
            return false;
        }
        return project.getEndDate() == null || !date.isAfter(project.getEndDate());
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private void requireInProject(LocalDate date) {
        if (date == null || !isWithinProject(date)) {
            throw InvalidMealAdjustmentException.dateOutsideProject(date,
                    project == null ? null : project.getStartDate(), project == null ? null : project.getEndDate());
        }
    }

    /**
     * Regras do ajuste, nesta ordem: tipo x dia da semana, quantidades, entregas e refrigerantes. Sem
     * fornecimento nao tem numeros. No dia extra e no dia alterado, os numeros de refeicao nao servida pelo
     * tipo sao descartados.
     */
    private MealAdjustment accept(MealAdjustment adjustment) {
        LocalDate date = adjustment.getDate();
        boolean weekend = isWeekend(date);
        switch (adjustment.getType()) {
            case EXTRA_DAY -> {
                if (!weekend) {
                    throw InvalidMealAdjustmentException.extraDayOnWeekday(date);
                }
            }
            case CHANGED -> {
                if (weekend) {
                    throw InvalidMealAdjustmentException.changedDayOnWeekend(date);
                }
            }
            case NO_SERVICE -> {
                if (weekend) {
                    throw InvalidMealAdjustmentException.noServiceOnWeekend(date);
                }
                if (adjustment.getLunchQuantity() != null || adjustment.getDinnerQuantity() != null
                        || adjustment.getLunchDeliveries() != null || adjustment.getDinnerDeliveries() != null
                        || adjustment.getSodas() != null) {
                    throw InvalidMealAdjustmentException.invalidQuantity("No service must not have quantities");
                }
                return adjustment;
            }
        }
        boolean lunch = mealType.servesLunch();
        boolean dinner = mealType.servesDinner();
        Integer lunchMeals = lunch ? validateAdjustmentQuantity(adjustment.getLunchQuantity(), "Lunch") : null;
        Integer dinnerMeals = dinner ? validateAdjustmentQuantity(adjustment.getDinnerQuantity(), "Dinner") : null;
        if ((lunchMeals == null || lunchMeals == 0) && (dinnerMeals == null || dinnerMeals == 0)) {
            throw InvalidMealAdjustmentException.invalidQuantity(
                    "Adjustment must have at least one quantity greater than zero");
        }
        Integer lunchDeliveries = lunch ? validateDeliveries(adjustment.getLunchDeliveries(), lunchMeals) : null;
        Integer dinnerDeliveries = dinner
                ? validateDeliveries(adjustment.getDinnerDeliveries(), dinnerMeals) : null;
        Integer sodas = adjustment.getSodas();
        if (sodas != null && (sodas < 0 || sodas > MAX_QUANTITY)) {
            throw InvalidMealAdjustmentException.invalidSodas(sodas);
        }
        if (sodas != null && sodas > 0 && getSodaPrice() == null) {
            throw InvalidMealAdjustmentException.sodaNotSupplied(sodas);
        }
        if (Objects.equals(lunchMeals, adjustment.getLunchQuantity())
                && Objects.equals(dinnerMeals, adjustment.getDinnerQuantity())
                && Objects.equals(lunchDeliveries, adjustment.getLunchDeliveries())
                && Objects.equals(dinnerDeliveries, adjustment.getDinnerDeliveries())) {
            return adjustment;
        }
        return adjustment.withNumbers(lunchMeals, dinnerMeals, lunchDeliveries, dinnerDeliveries, sodas);
    }

    private static Integer validateAdjustmentQuantity(Integer quantity, String meal) {
        if (quantity == null || quantity < 0 || quantity > MAX_QUANTITY) {
            throw InvalidMealAdjustmentException.invalidQuantity(
                    meal + " quantity of an adjustment must be between 0 and 10000: " + quantity);
        }
        return quantity;
    }

    /** Opcional (vazio = padrao), de 0 a 100, e zero quando a refeicao nao tem quantidade no dia. */
    private static Integer validateDeliveries(Integer deliveries, Integer meals) {
        if (deliveries == null) {
            return null;
        }
        if (deliveries < 0 || deliveries > MAX_DELIVERIES) {
            throw InvalidMealAdjustmentException.invalidDeliveries(deliveries);
        }
        if (deliveries > 0 && (meals == null || meals == 0)) {
            throw InvalidMealAdjustmentException.deliveryWithoutMeal(deliveries);
        }
        return deliveries;
    }

    private BigDecimal perDelivery(BigDecimal unitPrice) {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(getDeliveriesPerDay()));
    }

    private static Integer validateQuantity(Integer quantity, Function<Integer, InvalidMealException> error) {
        if (quantity == null || quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw error.apply(quantity);
        }
        return quantity;
    }

    private static BigDecimal validatePrice(BigDecimal price, Function<BigDecimal, InvalidMealException> error) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw error.apply(price);
        }
        return price;
    }

    /** Periodo de fechamento com os restaurantes, em dias uteis fixos (nao segue o calendario). */
    public enum BillingPeriod {
        DAY(1),
        WEEK(5),
        FORTNIGHT(10),
        MONTH(22);

        private final int workingDays;

        BillingPeriod(int workingDays) {
            this.workingDays = workingDays;
        }

        public int getWorkingDays() {
            return workingDays;
        }
    }

    public enum MealType {
        LUNCH(true, false),
        DINNER(false, true),
        LUNCH_AND_DINNER(true, true);

        private final boolean lunch;
        private final boolean dinner;

        MealType(boolean lunch, boolean dinner) {
            this.lunch = lunch;
            this.dinner = dinner;
        }

        public boolean servesLunch() {
            return lunch;
        }

        public boolean servesDinner() {
            return dinner;
        }

        /** Uma entrega por refeicao servida: 1 para LUNCH e DINNER, 2 para LUNCH_AND_DINNER. */
        public int deliveriesPerDay() {
            return (lunch ? 1 : 0) + (dinner ? 1 : 0);
        }

        /** Os dois tipos ocupam a mesma cobertura (almoco ou jantar) de uma obra. */
        public boolean overlaps(MealType other) {
            return other != null && ((lunch && other.lunch) || (dinner && other.dinner));
        }
    }
}
