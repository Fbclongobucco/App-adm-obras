package com.longobuccodev.app_adm_obras.support;

import java.util.Random;

/**
 * Gera CPF e CNPJ ficticios com digito verificador valido, de forma deterministica
 * a partir de uma semente. Uso exclusivo dos testes.
 */
public final class FakeDocuments {

    private static final int[] CPF_FIRST_WEIGHTS = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CPF_SECOND_WEIGHTS = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private final Random random;

    public FakeDocuments(long seed) {
        this.random = new Random(seed);
    }

    public String nextCpf() {
        return withCheckDigits(randomDigits(9), CPF_FIRST_WEIGHTS, CPF_SECOND_WEIGHTS);
    }

    public String nextCnpj() {
        int[] base = randomDigits(12);
        // Filial 0001, como nos CNPJs de matriz
        base[8] = 0;
        base[9] = 0;
        base[10] = 0;
        base[11] = 1;
        return withCheckDigits(base, CNPJ_FIRST_WEIGHTS, CNPJ_SECOND_WEIGHTS);
    }

    private int[] randomDigits(int length) {
        int[] digits;
        do {
            digits = new int[length];
            for (int i = 0; i < length; i++) {
                digits[i] = random.nextInt(10);
            }
        } while (allEqual(digits));
        return digits;
    }

    private static String withCheckDigits(int[] base, int[] firstWeights, int[] secondWeights) {
        int[] digits = new int[base.length + 2];
        System.arraycopy(base, 0, digits, 0, base.length);
        digits[base.length] = checkDigit(digits, firstWeights);
        digits[base.length + 1] = checkDigit(digits, secondWeights);
        StringBuilder value = new StringBuilder(digits.length);
        for (int digit : digits) {
            value.append(digit);
        }
        return value.toString();
    }

    private static int checkDigit(int[] digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += digits[i] * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean allEqual(int[] digits) {
        for (int digit : digits) {
            if (digit != digits[0]) {
                return false;
            }
        }
        return true;
    }
}
