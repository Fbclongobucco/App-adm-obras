package com.longobuccodev.app_adm_obras.infra.controllers;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;
import java.util.Map;

/**
 * Traduz uma violacao de integridade do banco para o codigo de erro da API, pelo nome da
 * restricao (V1/V3/V4/V7). Cobre a corrida entre a verificacao do caso de uso e a gravacao.
 * A mensagem devolvida nunca carrega texto do banco.
 */
final class IntegrityViolationTranslator {

    static final String DEFAULT_CODE = "data.integrity_violation";

    private static final Map<String, String> CODES_BY_CONSTRAINT = Map.ofEntries(
            Map.entry("uk_project_os", "project.duplicate.os"),
            Map.entry("uk_client_email", "client.duplicate.email"),
            Map.entry("uk_employee_email", "employee.duplicate.email"),
            Map.entry("uk_employee_cpf", "employee.duplicate.cpf"),
            Map.entry("uk_app_user_email", "user.duplicate.email"),
            Map.entry("uk_meal_project_lunch", "meal.duplicate.type"),
            Map.entry("uk_meal_project_dinner", "meal.duplicate.type"),
            Map.entry("uk_meal_adjustment_meal_date", "meal.adjustment.duplicate.date"),
            Map.entry("fk_employee_cost_center", "cost_center.in_use"),
            Map.entry("fk_project_cost_center", "cost_center.in_use"),
            Map.entry("fk_project_client", "client.in_use"),
            Map.entry("fk_accommodation_project", "project.in_use"),
            Map.entry("fk_meal_project", "project.in_use"),
            Map.entry("fk_restaurant_project", "project.in_use"),
            Map.entry("fk_meal_restaurant", "restaurant.in_use"),
            Map.entry("fk_project_employee_employee", "employee.in_use"),
            Map.entry("fk_accommodation_employee_employee", "employee.in_use"));

    private IntegrityViolationTranslator() {
    }

    static String errorCode(DataIntegrityViolationException ex) {
        String details = describe(ex);
        String matched = null;
        for (String constraint : CODES_BY_CONSTRAINT.keySet()) {
            if (details.contains(constraint) && (matched == null || constraint.length() > matched.length())) {
                matched = constraint;
            }
        }
        return matched == null ? DEFAULT_CODE : CODES_BY_CONSTRAINT.get(matched);
    }

    static String message(String errorCode) {
        if (errorCode.contains(".duplicate.")) {
            return "Another record already uses this value";
        }
        if (errorCode.endsWith(".in_use")) {
            return "Record is still referenced by other records";
        }
        return "Operation conflicts with existing data";
    }

    private static String describe(Throwable ex) {
        StringBuilder text = new StringBuilder();
        for (Throwable cause = ex; cause != null && text.length() < 10_000; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && violation.getConstraintName() != null) {
                text.append(' ').append(violation.getConstraintName());
            }
            if (cause.getMessage() != null) {
                text.append(' ').append(cause.getMessage());
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }
}
