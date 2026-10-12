package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.project.ProjectResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.ProjectUseCase;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    public ProjectController(ProjectUseCase projectUseCase) {
        this.projectUseCase = projectUseCase;
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> create(@RequestBody ProjectRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectUseCase.create(dto));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ProjectResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenacao no formato campo,direcao (asc|desc; padrao asc). "
                    + "Campos: updatedAt, createdAt, os, startDate. Ex.: updatedAt,desc")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Busca sem diferenciar maiusculas nem acentos em OS, descricao e nome do cliente. "
                    + "Ate 100 caracteres (400 page.invalid.query).")
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(projectUseCase.findAll(new PageRequest(page, size), sort, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectUseCase.findById(id));
    }

    @Operation(summary = "Substitui a obra",
            description = "Substituicao completa: employeeIds e accommodationIds omitidos ou nulos desfazem os "
                    + "vinculos atuais; envie sempre a lista completa. Refeicoes nao fazem parte da requisicao: "
                    + "vinculam-se a obra pelo restaurante e sao mantidas. A resposta traz meals, com dailyPrice, "
                    + "workingDays e totalPrice recalculados pelas datas novas da obra.")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> update(@PathVariable UUID id, @RequestBody ProjectRequestDTO dto) {
        return ResponseEntity.ok(projectUseCase.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
