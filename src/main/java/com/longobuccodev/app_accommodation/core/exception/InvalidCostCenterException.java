package com.longobuccodev.app_accommodation.core.exception;

public class InvalidCostCenterException extends CoreDomainException {

    private InvalidCostCenterException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidCostCenterException blankName() {
        return new InvalidCostCenterException("cost_center.invalid.name.blank", "Cost center name must not be blank");
    }

    public static InvalidCostCenterException shortName(String name) {
        return new InvalidCostCenterException(
                "cost_center.invalid.name.length",
                "Cost center name must be at least 3 characters long: " + name
        );
    }

    public static InvalidCostCenterException longName(String name) {
        return new InvalidCostCenterException(
                "cost_center.invalid.name.length",
                "Cost center name must not exceed 100 characters: " + name
        );
    }

    public static InvalidCostCenterException invalidCnpj(String cnpj) {
        return new InvalidCostCenterException("cost_center.invalid.cnpj", "Cost center CNPJ is invalid: " + cnpj);
    }
}