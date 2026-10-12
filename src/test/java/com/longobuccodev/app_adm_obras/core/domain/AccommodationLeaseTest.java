package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.ConflictException;
import com.longobuccodev.app_adm_obras.core.exception.InvalidAccommodationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locacao mensal da hospedagem (change hospedagem-locacao-mensal). */
class AccommodationLeaseTest {

    private static final LocalDate ENTRY = LocalDate.of(2026, 10, 12);

    private static Accommodation lease(LocalDate start, String monthly) {
        return new Accommodation(null, "Pousada Vale", "11988887777",
                new Address(null, "Rua A", "1", "Campinas", "SP", "Brasil", "Centro", "13010000"),
                4, false, null, start, new BigDecimal(monthly));
    }

    @Test
    @DisplayName("primeiro periodo: da entrada ate a vespera do mesmo dia no mes seguinte")
    void firstPeriod() {
        Accommodation accommodation = lease(ENTRY, "3000.00");

        assertThat(accommodation.getPeriods()).singleElement().satisfies(period -> {
            assertThat(period.getStartDate()).isEqualTo(ENTRY);
            assertThat(period.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 11));
            assertThat(period.getDays()).isEqualTo(31);
            assertThat(period.getPrice()).isEqualByComparingTo("3000.00");
        });
        assertThat(accommodation.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(accommodation.getTotalPrice()).isEqualByComparingTo("3000.00");
        assertThat(accommodation.isRenewed()).isFalse();
    }

    @Test
    @DisplayName("fim de mes: dia inexistente no mes seguinte termina no ultimo dia dele")
    void monthEnd() {
        assertThat(Accommodation.monthEnd(LocalDate.of(2027, 1, 31))).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(Accommodation.monthEnd(LocalDate.of(2028, 1, 30))).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(Accommodation.monthEnd(LocalDate.of(2026, 12, 1))).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(lease(LocalDate.of(2027, 1, 31), "100.00").getEndDate()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    @DisplayName("renovacao sugerida, renovacao editada e total como soma dos periodos")
    void renewals() {
        Accommodation accommodation = lease(ENTRY, "3000.00");

        AccommodationPeriod suggested = accommodation.nextRenewal();
        assertThat(suggested.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(suggested.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 11));
        assertThat(suggested.getPrice()).isEqualByComparingTo("3000.00");

        accommodation.renew(null, null);
        AccommodationPeriod fifteenDays = accommodation.renew(LocalDate.of(2026, 12, 26), new BigDecimal("1600.00"));

        assertThat(fifteenDays.getStartDate()).isEqualTo(LocalDate.of(2026, 12, 12));
        assertThat(fifteenDays.getDays()).isEqualTo(15);
        assertThat(accommodation.getPeriods()).hasSize(3);
        assertThat(accommodation.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 26));
        assertThat(accommodation.getTotalPrice()).isEqualByComparingTo("7600.00");
        assertThat(accommodation.isRenewed()).isTrue();
    }

    @Test
    @DisplayName("renovacao: data final antes do inicio e valor invalido sao rejeitados sem mudar nada")
    void invalidRenewal() {
        Accommodation accommodation = lease(ENTRY, "3000.00");

        assertThatThrownBy(() -> accommodation.renew(LocalDate.of(2026, 11, 11), null))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.renewal_end");
        assertThatThrownBy(() -> accommodation.renew(null, BigDecimal.ZERO))
                .isInstanceOf(InvalidAccommodationException.class)
                .extracting("errorCode").isEqualTo("accommodation.invalid.renewal_price");
        assertThat(accommodation.renew(LocalDate.of(2026, 11, 12), null).getDays()).as("um dia e valido").isEqualTo(1);
        assertThat(accommodation.getPeriods()).hasSize(2);
    }

    @Test
    @DisplayName("desfazer remove a ultima renovacao, mas nunca o primeiro periodo")
    void undoRenewal() {
        Accommodation accommodation = lease(ENTRY, "3000.00");
        accommodation.renew(null, null);
        accommodation.renew(null, null);

        accommodation.undoRenewal();
        accommodation.undoRenewal();

        assertThat(accommodation.getPeriods()).hasSize(1);
        assertThat(accommodation.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThatThrownBy(accommodation::undoRenewal)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("accommodation.lease.first_period");
    }

    @Test
    @DisplayName("edicao: sem renovacao refaz o primeiro periodo; com renovacao a entrada fica bloqueada")
    void editLease() {
        Accommodation accommodation = lease(ENTRY, "3000.00");
        String firstId = accommodation.getPeriods().getFirst().getId().toString();

        accommodation.configureLease(LocalDate.of(2026, 10, 15), new BigDecimal("3200.00"));

        assertThat(accommodation.getPeriods()).singleElement().satisfies(period -> {
            assertThat(period.getId().toString()).as("mantem o id").isEqualTo(firstId);
            assertThat(period.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 15));
            assertThat(period.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 14));
            assertThat(period.getPrice()).isEqualByComparingTo("3200.00");
        });

        accommodation.renew(null, null);
        assertThatThrownBy(() -> accommodation.configureLease(ENTRY, new BigDecimal("3200.00")))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("accommodation.lease.renewed");

        accommodation.configureLease(LocalDate.of(2026, 10, 15), new BigDecimal("3500.00"));
        assertThat(accommodation.getMonthlyPrice()).isEqualByComparingTo("3500.00");
        assertThat(accommodation.getPeriods().getFirst().getPrice()).as("periodos gravados nao mudam")
                .isEqualByComparingTo("3200.00");
        assertThat(accommodation.nextRenewal().getPrice()).isEqualByComparingTo("3500.00");
    }
}
