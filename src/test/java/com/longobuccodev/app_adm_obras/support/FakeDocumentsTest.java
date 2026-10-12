package com.longobuccodev.app_adm_obras.support;

import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FakeDocumentsTest {

    @Test
    @DisplayName("todo CPF e CNPJ gerado deve passar nas validacoes do dominio")
    void generatedDocumentsShouldPassDomainValidation() {
        FakeDocuments documents = new FakeDocuments(42L);
        CostCenter costCenter = new CostCenter(null, "Centro Norte", "11222333000181");

        for (int i = 0; i < 500; i++) {
            String cpf = documents.nextCpf();
            String cnpj = documents.nextCnpj();
            assertThatCode(() -> new Employee(null, "Colaborador Teste", "c@exemplo.test", cpf, null, null,
                    LocalDate.of(1990, 1, 1), costCenter, Employee.Role.MONTADOR)).doesNotThrowAnyException();
            assertThatCode(() -> new CostCenter(null, "Centro Teste", cnpj)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("a mesma semente deve gerar a mesma sequencia")
    void sameSeedShouldGenerateSameSequence() {
        FakeDocuments first = new FakeDocuments(7L);
        FakeDocuments second = new FakeDocuments(7L);
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            a.add(first.nextCpf() + first.nextCnpj());
            b.add(second.nextCpf() + second.nextCnpj());
        }

        assertThat(a).isEqualTo(b);
    }
}
