package com.longobuccodev.app_adm_obras.core.domain;

import com.longobuccodev.app_adm_obras.core.exception.InvalidCostCenterException;

import java.util.Objects;
import java.util.UUID;

public class CostCenter {

    private static final int CNPJ_LENGTH = 14;
    private static final int[] CNPJ_FIRST_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_SECOND_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 100;

    private UUID id;
    private String name;
    private String cnpj;

    public CostCenter(UUID id, String name, String cnpj) {
        setId(id);
        setName(name);
        setCnpj(cnpj);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateName(name);
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = validateCnpj(cnpj);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof CostCenter costCenter && Objects.equals(id, costCenter.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw InvalidCostCenterException.blankName();
        }
        String normalized = name.strip().replaceAll("\\s+", " ");
        if (normalized.length() < MIN_NAME_LENGTH) {
            throw InvalidCostCenterException.shortName(normalized);
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw InvalidCostCenterException.longName(normalized);
        }
        return normalized;
    }

    private static String validateCnpj(String cnpj) {
        if (cnpj == null) {
            throw InvalidCostCenterException.invalidCnpj(cnpj);
        }
        String digits = cnpj.replaceAll("[^0-9]", "");
        if (!isValidCnpj(digits)) {
            throw InvalidCostCenterException.invalidCnpj(cnpj);
        }
        return digits;
    }

    private static boolean isValidCnpj(String digits) {
        if (digits.length() != CNPJ_LENGTH || digits.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigitSum = 0;
        for (int index = 0; index < CNPJ_FIRST_DIGIT_WEIGHTS.length; index++) {
            firstDigitSum += Character.getNumericValue(digits.charAt(index)) * CNPJ_FIRST_DIGIT_WEIGHTS[index];
        }
        if (checkDigit(firstDigitSum) != Character.getNumericValue(digits.charAt(12))) {
            return false;
        }

        int secondDigitSum = 0;
        for (int index = 0; index < CNPJ_SECOND_DIGIT_WEIGHTS.length; index++) {
            secondDigitSum += Character.getNumericValue(digits.charAt(index)) * CNPJ_SECOND_DIGIT_WEIGHTS[index];
        }
        return checkDigit(secondDigitSum) == Character.getNumericValue(digits.charAt(13));
    }

    private static int checkDigit(int sum) {
        int remainder = (sum * 10) % 11;
        return remainder >= 10 ? 0 : remainder;
    }
}