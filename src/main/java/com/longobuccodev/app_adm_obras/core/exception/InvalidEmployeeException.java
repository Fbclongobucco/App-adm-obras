package com.longobuccodev.app_adm_obras.core.exception;

import java.time.LocalDate;

public class InvalidEmployeeException extends CoreDomainException {

    private InvalidEmployeeException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidEmployeeException blankName() {
        return new InvalidEmployeeException("employee.invalid.name.blank", "Employee name must not be blank");
    }

    public static InvalidEmployeeException shortName(String name) {
        return new InvalidEmployeeException(
                "employee.invalid.name.length",
                "Employee name must be at least 3 characters long: " + name
        );
    }

    public static InvalidEmployeeException longName(String name) {
        return new InvalidEmployeeException(
                "employee.invalid.name.length",
                "Employee name must not exceed 100 characters: " + name
        );
    }

    public static InvalidEmployeeException invalidEmail(String email) {
        return new InvalidEmployeeException("employee.invalid.email", "Employee email is invalid: " + email);
    }

    public static InvalidEmployeeException invalidCpf(String cpf) {
        return new InvalidEmployeeException("employee.invalid.cpf", "Employee CPF is invalid: " + cpf);
    }

    public static InvalidEmployeeException invalidPhone(String phone) {
        return new InvalidEmployeeException(
                "employee.invalid.phone",
                "Employee phone must have 10 or 11 digits: " + phone
        );
    }

    public static InvalidEmployeeException invalidBirthDate(LocalDate birthDate) {
        return new InvalidEmployeeException(
                "employee.invalid.birth_date",
                "Employee birth date must not be null nor in the future: " + birthDate
        );
    }

    public static InvalidEmployeeException missingRole() {
        return new InvalidEmployeeException("employee.invalid.role", "Employee role must not be null");
    }

    public static InvalidEmployeeException missingCostCenter() {
        return new InvalidEmployeeException(
                "employee.invalid.cost_center",
                "Employee cost center must not be null"
        );
    }
}