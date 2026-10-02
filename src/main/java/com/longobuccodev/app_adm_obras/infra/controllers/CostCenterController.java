package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.costcenter.CostCenterResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.CostCenterUseCase;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/cost-centers")
public class CostCenterController {

    private final CostCenterUseCase costCenterUseCase;

    public CostCenterController(CostCenterUseCase costCenterUseCase) {
        this.costCenterUseCase = costCenterUseCase;
    }

    @PostMapping
    public ResponseEntity<CostCenterResponseDTO> create(@RequestBody CostCenterRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(costCenterUseCase.create(dto));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<CostCenterResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(costCenterUseCase.findAll(new PageRequest(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostCenterResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(costCenterUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CostCenterResponseDTO> update(@PathVariable UUID id,
                                                        @RequestBody CostCenterRequestDTO dto) {
        return ResponseEntity.ok(costCenterUseCase.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        costCenterUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
