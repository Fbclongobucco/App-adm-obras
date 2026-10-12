package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationRenewalRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.AccommodationUseCase;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Busca sem diferenciar maiusculas nem acentos em anfitriao, cidade e OS da obra. "
                    + "Ate 100 caracteres (400 page.invalid.query).")
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(accommodationUseCase.findAll(new PageRequest(page, size), q));
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

    @Operation(summary = "Renova a locacao da hospedagem",
            description = "Novo periodo a partir do dia seguinte ao termino. endDate e price sao opcionais: ausentes, "
                    + "usam a sugestao (nextRenewal: +1 mes, vespera do mesmo dia, pelo valor mensal). 400 "
                    + "accommodation.invalid.renewal_end (data final antes do inicio) ou "
                    + "accommodation.invalid.renewal_price (valor <= 0). 201 com a hospedagem atualizada.")
    @PostMapping("/{id}/renewals")
    public ResponseEntity<AccommodationResponseDTO> renew(
            @PathVariable UUID id, @RequestBody(required = false) AccommodationRenewalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accommodationUseCase.renew(id, dto));
    }

    @Operation(summary = "Desfaz a ultima renovacao da hospedagem",
            description = "204 sem corpo. 409 accommodation.lease.first_period quando so ha o primeiro periodo.")
    @DeleteMapping("/{id}/renewals/last")
    public ResponseEntity<Void> undoRenewal(@PathVariable UUID id) {
        accommodationUseCase.undoRenewal(id);
        return ResponseEntity.noContent().build();
    }
}
