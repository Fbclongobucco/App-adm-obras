package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.exception.InvalidAccommodationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Hospedagem locada por mes: a entrada e o valor mensal definem o primeiro periodo, e cada renovacao
 * acrescenta um periodo a partir do dia seguinte ao termino. O total e a soma dos periodos.
 */
public class Accommodation extends Audited {

    private static final int MIN_HOST_NAME_LENGTH = 3;
    private static final int MAX_HOST_NAME_LENGTH = 100;
    private static final int MIN_PHONE_DIGITS = 10;
    private static final int MAX_PHONE_DIGITS = 11;
    private static final int MIN_CAPACITY = 1;
    private static final int MAX_CAPACITY = 1000;

    private UUID id;
    private String hostName;
    private String hostPhone;
    private Address address;
    private Integer capacity;
    private Boolean isContract;
    private Project project;
    private final Set<Employee> employees = new LinkedHashSet<>();
    private LocalDate startDate;
    private BigDecimal monthlyPrice;
    private final List<AccommodationPeriod> periods = new ArrayList<>();

    /** Cria o primeiro periodo, da entrada ate o fim do mes de locacao, pelo valor mensal. */
    public Accommodation(UUID id, String hostName, String hostPhone, Address address, Integer capacity,
                         Boolean isContract, Project project, LocalDate startDate, BigDecimal monthlyPrice) {
        setId(id);
        setHostName(hostName);
        setHostPhone(hostPhone);
        setAddress(address);
        setCapacity(capacity);
        setContract(isContract);
        configureLease(startDate, monthlyPrice);
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
        return Collections.unmodifiableSet(employees);
    }

    public void addEmployee(Employee employee) {
        if (employee == null) {
            throw InvalidAccommodationException.missingEmployee();
        }
        employees.add(employee);
    }

    public void removeEmployee(Employee employee) {
        employees.remove(employee);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    /** Valor do mes de locacao; base do primeiro periodo e sugestao das renovacoes. */
    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    /** Periodos em ordem de inicio; o primeiro comeca na entrada. */
    public List<AccommodationPeriod> getPeriods() {
        return Collections.unmodifiableList(periods);
    }

    /** Fim do ultimo periodo. */
    public LocalDate getEndDate() {
        return periods.getLast().getEndDate();
    }

    /** Soma dos periodos. */
    public BigDecimal getTotalPrice() {
        return periods.stream().map(AccommodationPeriod::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isRenewed() {
        return periods.size() > 1;
    }

    /**
     * Fim de um mes de locacao: vespera do mesmo dia no mes seguinte; quando o mes seguinte nao tem
     * esse dia, o ultimo dia dele (31/01 -> 28/02).
     */
    public static LocalDate monthEnd(LocalDate start) {
        LocalDate next = start.plusMonths(1);
        return next.getDayOfMonth() == start.getDayOfMonth() ? next.minusDays(1) : next;
    }

    /**
     * Define entrada e valor mensal. Sem renovacoes, refaz o primeiro periodo (mantendo id e auditoria).
     * Com renovacoes, a entrada nao muda (409) e o valor mensal vale so como sugestao das proximas.
     */
    public void configureLease(LocalDate newStart, BigDecimal newMonthlyPrice) {
        if (newStart == null) {
            throw InvalidAccommodationException.invalidStartDate();
        }
        if (newMonthlyPrice == null || newMonthlyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidAccommodationException.invalidMonthlyPrice(newMonthlyPrice);
        }
        if (isRenewed()) {
            if (!newStart.equals(startDate)) {
                throw ConflictException.of("accommodation.lease.renewed",
                        "Undo the renewals to change the start date: " + startDate);
            }
            monthlyPrice = newMonthlyPrice;
            return;
        }
        LocalDate end = monthEnd(newStart);
        AccommodationPeriod first = periods.isEmpty()
                ? new AccommodationPeriod(null, newStart, end, newMonthlyPrice)
                : periods.getFirst().with(newStart, end, newMonthlyPrice);
        periods.clear();
        periods.add(first);
        startDate = newStart;
        monthlyPrice = newMonthlyPrice;
    }

    /** Sugestao da proxima renovacao: um mes a partir do dia seguinte ao termino, pelo valor mensal. */
    public AccommodationPeriod nextRenewal() {
        LocalDate start = getEndDate().plusDays(1);
        return new AccommodationPeriod(null, start, monthEnd(start), monthlyPrice);
    }

    /**
     * Renova a partir do dia seguinte ao termino. Data final e valor ausentes usam a sugestao.
     *
     * @return o periodo incluido
     */
    public AccommodationPeriod renew(LocalDate endDate, BigDecimal price) {
        AccommodationPeriod suggestion = nextRenewal();
        LocalDate end = endDate == null ? suggestion.getEndDate() : endDate;
        BigDecimal value = price == null ? suggestion.getPrice() : price;
        if (end.isBefore(suggestion.getStartDate())) {
            throw InvalidAccommodationException.invalidRenewalEnd(suggestion.getStartDate(), end);
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidAccommodationException.invalidRenewalPrice(value);
        }
        AccommodationPeriod period = new AccommodationPeriod(null, suggestion.getStartDate(), end, value);
        periods.add(period);
        return period;
    }

    /** Remove a ultima renovacao; o primeiro periodo nao sai (409). */
    public void undoRenewal() {
        if (!isRenewed()) {
            throw ConflictException.of("accommodation.lease.first_period",
                    "The first lease period cannot be removed");
        }
        periods.removeLast();
    }

    /**
     * Reidrata os periodos gravados (leitura do banco ou versao anterior na edicao), sem regras.
     * A entrada passa a ser o inicio do primeiro periodo.
     */
    public void restorePeriods(Collection<AccommodationPeriod> saved) {
        if (saved == null || saved.isEmpty()) {
            return;
        }
        periods.clear();
        periods.addAll(saved.stream().sorted(Comparator.comparing(AccommodationPeriod::getStartDate)).toList());
        startDate = periods.getFirst().getStartDate();
    }

    /** Auditoria dos periodos ainda nao gravados. */
    public void stampPeriods(Instant now) {
        periods.stream().filter(period -> period.getAudit() == null).forEach(period -> period.markCreated(now));
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
}
