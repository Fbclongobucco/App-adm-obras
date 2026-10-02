package com.longobuccodev.app_adm_obras.application.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    private ResourceNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static ResourceNotFoundException accommodation(UUID id) {
        return new ResourceNotFoundException("accommodation.not_found", "Accommodation not found: " + id);
    }

    public static ResourceNotFoundException project(UUID id) {
        return new ResourceNotFoundException("project.not_found", "Project not found: " + id);
    }

    public static ResourceNotFoundException employee(UUID id) {
        return new ResourceNotFoundException("employee.not_found", "Employee not found: " + id);
    }

    public static ResourceNotFoundException address(UUID id) {
        return new ResourceNotFoundException("address.not_found", "Address not found: " + id);
    }

    public static ResourceNotFoundException client(UUID id) {
        return new ResourceNotFoundException("client.not_found", "Client not found: " + id);
    }

    public static ResourceNotFoundException clientByProject(UUID projectId) {
        return new ResourceNotFoundException("client.not_found", "Client not found for project: " + projectId);
    }

    public static ResourceNotFoundException costCenter(UUID id) {
        return new ResourceNotFoundException("cost_center.not_found", "Cost center not found: " + id);
    }

    public static ResourceNotFoundException meal(UUID id) {
        return new ResourceNotFoundException("meal.not_found", "Meal not found: " + id);
    }

    public static ResourceNotFoundException user(UUID id) {
        return new ResourceNotFoundException("user.not_found", "User not found: " + id);
    }
}
