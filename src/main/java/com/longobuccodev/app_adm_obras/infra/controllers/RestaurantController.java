package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.closing.RestaurantAdjustmentRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.closing.WeeklyClosingResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.RestaurantUseCase;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantUseCase restaurantUseCase;

    public RestaurantController(RestaurantUseCase restaurantUseCase) {
        this.restaurantUseCase = restaurantUseCase;
    }

    @Operation(summary = "Cria o restaurante da obra",
            description = "deliveryFee: taxa por entrega, opcional (null = sem taxa); informada, deve ser maior "
                    + "que zero, senao 400 restaurant.invalid.delivery_fee. sodaPrice: preco unitario do "
                    + "refrigerante, 1 por entrega em todas as refeicoes do restaurante, opcional (null = nao "
                    + "fornece); informado, deve ser maior que zero, senao 400 restaurant.invalid.soda_price. "
                    + "Erros 400: restaurant.invalid.name, restaurant.invalid.phone, restaurant.invalid.project. "
                    + "A resposta traz meals, as refeicoes do restaurante no formato resumido, com custos e "
                    + "projections.")
    @PostMapping
    public ResponseEntity<RestaurantResponseDTO> create(@RequestBody RestaurantRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantUseCase.create(dto));
    }

    @Operation(summary = "Lista restaurantes",
            description = "Cada item traz meals (refeicoes resumidas do restaurante, com custos e projections); "
                    + "as refeicoes da pagina sao lidas numa unica consulta.")
    @GetMapping
    public ResponseEntity<PageResponseDTO<RestaurantResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Busca sem diferenciar maiusculas nem acentos em nome, OS da obra e cidade. "
                    + "Ate 100 caracteres (400 page.invalid.query).")
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(restaurantUseCase.findAll(new PageRequest(page, size), q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(restaurantUseCase.findById(id));
    }

    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<List<RestaurantResponseDTO>> findByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(restaurantUseCase.findByProjectId(projectId));
    }

    @Operation(summary = "Substitui o restaurante",
            description = "deliveryFee: taxa por entrega, opcional (null = sem taxa); informada, deve ser maior "
                    + "que zero, senao 400 restaurant.invalid.delivery_fee. sodaPrice: preco unitario do "
                    + "refrigerante, 1 por entrega em todas as refeicoes do restaurante, opcional (null = nao "
                    + "fornece); informado, deve ser maior que zero, senao 400 restaurant.invalid.soda_price; "
                    + "a alteracao vale para a leitura seguinte de todas as refeicoes do restaurante. "
                    + "Erros 400: restaurant.invalid.name, "
                    + "restaurant.invalid.phone, restaurant.invalid.project. Restaurante com refeicoes nao muda "
                    + "de obra (409 restaurant.in_use). A resposta traz meals, as refeicoes do restaurante no "
                    + "formato resumido, com custos e projections.")
    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponseDTO> update(@PathVariable UUID id,
                                                        @RequestBody RestaurantRequestDTO dto) {
        return ResponseEntity.ok(restaurantUseCase.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        restaurantUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Fechamento semanal do restaurante",
            description = "Soma dia a dia as refeicoes do restaurante. " + "Semanas de segunda a domingo com algum dia no periodo da obra (start e end cortados no periodo; "
                    + "partial quando cortada; current quando contem hoje, em UTC); sem termino, ate a semana atual. "
                    + "week: qualquer data da semana; ausente ou fora da lista usa a semana atual, a primeira (antes "
                    + "do inicio) ou a ultima (depois do termino), informada em selected. days: os 7 dias com status "
                    + "SERVICE, NO_SERVICE, EXTRA_DAY, OFF (fim de semana sem ajuste) ou OUT_OF_PERIOD, quantidades, "
                    + "price e adjustmentIds. totals.serviceDays conta SERVICE e EXTRA_DAY."
                    + " No restaurante: EXTRA_DAY se alguma refeicao tem dia extra; NO_SERVICE se todas estao sem "
                    + "fornecimento; SERVICE com partialNoService quando so parte esta sem. 404 restaurant.not_found.")
    @GetMapping("/{id}/weekly-closing")
    public ResponseEntity<WeeklyClosingResponseDTO> weeklyClosing(
            @PathVariable UUID id,
            @Parameter(description = "Qualquer data da semana (aaaa-mm-dd); opcional")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return ResponseEntity.ok(restaurantUseCase.weeklyClosing(id, week));
    }

    @Operation(summary = "Inclui um ajuste por data em todas as refeicoes do restaurante",
            description = "NO_SERVICE: em todas as refeicoes, sem quantidades. EXTRA_DAY: so nas refeicoes de "
                    + "quantities com alguma quantidade maior que zero; nenhuma -> 400 meal.adjustment.invalid.quantity. "
                    + "Mesmas regras e codigos do ajuste da refeicao; qualquer erro em uma refeicao nao grava nada "
                    + "(ex.: 409 meal.adjustment.duplicate.date). mealId de outra refeicao -> 404 meal.not_found. "
                    + "201 com os ajustes criados.")
    @PostMapping("/{id}/adjustments")
    public ResponseEntity<List<MealAdjustmentResponseDTO>> addAdjustment(
            @PathVariable UUID id, @RequestBody RestaurantAdjustmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantUseCase.addAdjustment(id, dto));
    }

    @Operation(summary = "Remove os ajustes da data em todas as refeicoes do restaurante",
            description = "204 sem corpo. 404 meal.adjustment.not_found quando nenhuma refeicao tem ajuste na data.")
    @DeleteMapping("/{id}/adjustments/{date}")
    public ResponseEntity<Void> removeAdjustments(
            @PathVariable UUID id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        restaurantUseCase.removeAdjustments(id, date);
        return ResponseEntity.noContent().build();
    }
}
