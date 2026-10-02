package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.employee.EmployeeResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.EmployeeUseCase;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeUseCase employeeUseCase;

    public EmployeeController(EmployeeUseCase employeeUseCase) {
        this.employeeUseCase = employeeUseCase;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponseDTO> create(@RequestBody EmployeeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeUseCase.create(dto));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<EmployeeResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(employeeUseCase.findAll(new PageRequest(page, size)));
    }

    @GetMapping("/by-cost-center/{costCenterId}")
    public ResponseEntity<List<EmployeeResponseDTO>> findByCostCenterId(@PathVariable UUID costCenterId) {
        return ResponseEntity.ok(employeeUseCase.findByCostCenterId(costCenterId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> update(@PathVariable UUID id, @RequestBody EmployeeRequestDTO dto) {
        return ResponseEntity.ok(employeeUseCase.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        employeeUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
