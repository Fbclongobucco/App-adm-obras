package com.longobuccodev.app_accommodation.core.domain;

import com.longobuccodev.app_accommodation.core.exception.InvalidCostCenterException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostCenterTest {

    private static CostCenter newCostCenter() {
        return new CostCenter(null, "Obra  Sao Paulo ", "11.222.333/0001-81");
    }

    @Test
    void shouldNormalizeFieldsOnConstructor() {
        CostCenter costCenter = newCostCenter();

        assertThat(costCenter.getId()).isNotNull();
        assertThat(costCenter.getName()).isEqualTo("Obra Sao Paulo");
        assertThat(costCenter.getCnpj()).isEqualTo("11222333000181");
    }

    @Test
    void shouldKeepProvidedIdAndGenerateWhenNull() {
        UUID id = UUID.randomUUID();
        CostCenter costCenter = newCostCenter();

        assertThat(new CostCenter(id, "Obra Sao Paulo", "11.222.333/0001-81").getId()).isEqualTo(id);

        costCenter.setId(null);

        assertThat(costCenter.getId()).isNotNull();
    }

    @Test
    void shouldCompareById() {
        UUID id = UUID.randomUUID();

        assertThat(new CostCenter(id, "Obra Sao Paulo", "11.222.333/0001-81"))
                .isEqualTo(new CostCenter(id, "Obra Rio", "11.222.333/0002-62"));
        assertThat(new CostCenter(id, "Obra Sao Paulo", "11.222.333/0001-81")).isNotEqualTo(newCostCenter());
    }

    @Test
    void shouldRejectBlankName() {
        CostCenter costCenter = newCostCenter();

        assertThatThrownBy(() -> costCenter.setName("  "))
                .isInstanceOf(InvalidCostCenterException.class)
                .extracting("errorCode").isEqualTo("cost_center.invalid.name.blank");
    }

    @Test
    void shouldRejectShortName() {
        CostCenter costCenter = newCostCenter();

        assertThatThrownBy(() -> costCenter.setName("CC"))
                .isInstanceOf(InvalidCostCenterException.class)
                .extracting("errorCode").isEqualTo("cost_center.invalid.name.length");
    }

    @Test
    void shouldRejectLongName() {
        CostCenter costCenter = newCostCenter();

        assertThatThrownBy(() -> costCenter.setName("a".repeat(101)))
                .isInstanceOf(InvalidCostCenterException.class)
                .extracting("errorCode").isEqualTo("cost_center.invalid.name.length");
    }

    @Test
    void shouldRejectCnpjWithWrongCheckDigits() {
        CostCenter costCenter = newCostCenter();

        assertThatThrownBy(() -> costCenter.setCnpj("11.222.333/0001-82"))
                .isInstanceOf(InvalidCostCenterException.class)
                .extracting("errorCode").isEqualTo("cost_center.invalid.cnpj");
    }

    @Test
    void shouldRejectCnpjWithRepeatedDigits() {
        CostCenter costCenter = newCostCenter();

        assertThatThrownBy(() -> costCenter.setCnpj("11.111.111/1111-11"))
                .isInstanceOf(InvalidCostCenterException.class)
                .extracting("errorCode").isEqualTo("cost_center.invalid.cnpj");
    }

    @Test
    void shouldRejectNullCnpj() {
        CostCenter costCenter = newCostCenter();

        assertThatThrownBy(() -> costCenter.setCnpj(null))
                .isInstanceOf(InvalidCostCenterException.class)
                .extracting("errorCode").isEqualTo("cost_center.invalid.cnpj");
    }

    @Test
    void shouldAcceptCnpjWithoutMask() {
        CostCenter costCenter = newCostCenter();

        assertThatNoException().isThrownBy(() -> costCenter.setCnpj("11222333000181"));
    }

    @Test
    void shouldValidateOnConstructor() {
        assertThatThrownBy(() -> new CostCenter(null, null, "11.222.333/0001-81"))
                .isInstanceOf(InvalidCostCenterException.class);
    }
}