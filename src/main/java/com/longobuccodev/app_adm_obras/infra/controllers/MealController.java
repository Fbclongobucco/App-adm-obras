package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.MealUseCase;
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
@RequestMapping("/api/meals")
public class MealController {

    private final MealUseCase mealUseCase;

    public MealController(MealUseCase mealUseCase) {
        this.mealUseCase = mealUseCase;
    }

    @PostMapping
    public ResponseEntity<MealResponseDTO> create(@RequestBody MealRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealUseCase.create(dto));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<MealResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mealUseCase.findAll(new PageRequest(page, size)));
    }

    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<List<MealResponseDTO>> findByProjectId(@PathVariable UUID projectId) {
        return ResponseEntity.ok(mealUseCase.findByProjectId(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mealUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealResponseDTO> update(@PathVariable UUID id, @RequestBody MealRequestDTO dto) {
        return ResponseEntity.ok(mealUseCase.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        mealUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
