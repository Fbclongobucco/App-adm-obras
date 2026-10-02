package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.AccommodationUseCase;
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
@RequestMapping("/api/accommodations")
public class AccommodationController {

    private final AccommodationUseCase accommodationUseCase;

    public AccommodationController(AccommodationUseCase accommodationUseCase) {
        this.accommodationUseCase = accommodationUseCase;
    }

    @PostMapping
    public ResponseEntity<AccommodationResponseDTO> create(@RequestBody AccommodationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accommodationUseCase.create(dto));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<AccommodationResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(accommodationUseCase.findAll(new PageRequest(page, size)));
    }

    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<List<AccommodationResponseDTO>> findByProjectId(@PathVariable UUID projectId) {
        return ResponseEntity.ok(accommodationUseCase.findByProjectId(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccommodationResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(accommodationUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccommodationResponseDTO> update(@PathVariable UUID id,
                                                           @RequestBody AccommodationRequestDTO dto) {
        return ResponseEntity.ok(accommodationUseCase.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accommodationUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
