package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.closing.WeeklyClosingResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealAdjustmentResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.meal.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.MealUseCase;
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
@RequestMapping("/api/meals")
public class MealController {

    private static final String ADJUSTMENT_RULES = "type: EXTRA_DAY (sabado ou domingo com fornecimento, soma ao "
            + "total) ou NO_SERVICE (dia util sem fornecimento, desconta um dia util). Regras, nesta ordem: data "
            + "dentro do periodo da obra, >= inicio e <= termino quando houver (400 meal.adjustment.invalid.date); "
            + "uma data por refeicao (409 meal.adjustment.duplicate.date); EXTRA_DAY so sabado ou domingo (400 "
            + "meal.adjustment.invalid.extra_day); NO_SERVICE so de segunda a sexta (400 "
            + "meal.adjustment.invalid.no_service); EXTRA_DAY exige lunchQuantity/dinnerQuantity das refeicoes "
            + "servidas pelo tipo, inteiros de 0 a 10000 com ao menos um maior que zero, e ignora o da refeicao "
            + "nao servida; NO_SERVICE nao aceita quantidades (400 meal.adjustment.invalid.quantity). type ausente: "
            + "400 meal.adjustment.invalid.type; note ate 100 caracteres: 400 meal.adjustment.invalid.note. "
            + "Valor do dia extra: soma de quantidade x preco das refeicoes com quantidade > 0, mais uma entrega "
            + "por refeicao com quantidade > 0 x deliveryFee do restaurante, mais um refrigerante por entrega x "
            + "sodaPrice do restaurante quando houver. Resposta 201 com o ajuste.";

    private final MealUseCase mealUseCase;

    public MealController(MealUseCase mealUseCase) {
        this.mealUseCase = mealUseCase;
    }

    @Operation(summary = "Cria a refeicao da obra",
            description = "mealType: LUNCH, DINNER ou LUNCH_AND_DINNER. lunchQuantity/lunchPrice sao obrigatorios "
                    + "quando o tipo inclui almoco e dinnerQuantity/dinnerPrice quando inclui jantar (quantidade "
                    + "inteira de 1 a 10000, preco maior que zero); os da refeicao nao servida sao ignorados e "
                    + "devolvidos null. Erros 400: meal.invalid.restaurant, meal.invalid.type, "
                    + "meal.invalid.lunch_quantity, meal.invalid.lunch_price, meal.invalid.dinner_quantity, "
                    + "meal.invalid.dinner_price. A obra e a do restaurante e admite uma refeicao com almoco e uma "
                    + "com jantar (LUNCH_AND_DINNER ocupa as duas); sobreposicao responde 409 meal.duplicate.type. "
                    + "sodaPrice nao e lido (o refrigerante e do restaurante; ignorado se enviado). "
                    + "Resposta: sodaPrice = sodaPrice do restaurante; deliveriesPerDay = 1 por refeicao servida (2 em "
                    + "LUNCH_AND_DINNER); mealsDailyPrice = soma de quantidade x preco; deliveryDailyPrice = "
                    + "deliveriesPerDay x deliveryFee do restaurante (0 sem taxa); sodaDailyPrice = "
                    + "deliveriesPerDay x sodaPrice do restaurante (0 sem refrigerante); dailyPrice = soma das tres parcelas; "
                    + "workingDays = dias de segunda a sexta entre inicio e termino da obra, inclusive, sem "
                    + "feriados; serviceDays = workingDays - noServiceDays; noServiceDays/extraDays = ajustes NO_SERVICE/"
                    + "EXTRA_DAY no periodo atual da obra; extraDaysPrice = soma dos dias extras no periodo; "
                    + "totalPrice = dailyPrice x serviceDays + extraDaysPrice (null sem termino da obra); "
                    + "adjustments = ajustes por data, por data; projections = "
                    + "DAY (1), WEEK (5), FORTNIGHT (10) e MONTH (22 dias uteis) com lunchMeals, dinnerMeals, "
                    + "deliveries, sodas e price = dailyPrice x dias.")
    @PostMapping
    public ResponseEntity<MealResponseDTO> create(@RequestBody MealRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealUseCase.create(dto));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<MealResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Busca sem diferenciar maiusculas nem acentos em nome do restaurante e OS da obra. "
                    + "Ate 100 caracteres (400 page.invalid.query).")
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(mealUseCase.findAll(new PageRequest(page, size), q));
    }

    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<List<MealResponseDTO>> findByProjectId(@PathVariable UUID projectId) {
        return ResponseEntity.ok(mealUseCase.findByProjectId(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mealUseCase.findById(id));
    }

    @Operation(summary = "Substitui a refeicao",
            description = "mealType: LUNCH, DINNER ou LUNCH_AND_DINNER. lunchQuantity/lunchPrice sao obrigatorios "
                    + "quando o tipo inclui almoco e dinnerQuantity/dinnerPrice quando inclui jantar (quantidade "
                    + "inteira de 1 a 10000, preco maior que zero); os da refeicao nao servida sao ignorados e "
                    + "devolvidos null. Erros 400: meal.invalid.restaurant, meal.invalid.type, "
                    + "meal.invalid.lunch_quantity, meal.invalid.lunch_price, meal.invalid.dinner_quantity, "
                    + "meal.invalid.dinner_price. A obra e a do restaurante e admite uma refeicao com almoco e uma "
                    + "com jantar (LUNCH_AND_DINNER ocupa as duas); sobreposicao responde 409 meal.duplicate.type. "
                    + "sodaPrice nao e lido (o refrigerante e do restaurante; ignorado se enviado). "
                    + "Resposta: sodaPrice = sodaPrice do restaurante; deliveriesPerDay = 1 por refeicao servida (2 em "
                    + "LUNCH_AND_DINNER); mealsDailyPrice = soma de quantidade x preco; deliveryDailyPrice = "
                    + "deliveriesPerDay x deliveryFee do restaurante (0 sem taxa); sodaDailyPrice = "
                    + "deliveriesPerDay x sodaPrice do restaurante (0 sem refrigerante); dailyPrice = soma das tres parcelas; "
                    + "workingDays = dias de segunda a sexta entre inicio e termino da obra, inclusive, sem "
                    + "feriados; serviceDays = workingDays - noServiceDays; noServiceDays/extraDays = ajustes NO_SERVICE/"
                    + "EXTRA_DAY no periodo atual da obra; extraDaysPrice = soma dos dias extras no periodo; "
                    + "totalPrice = dailyPrice x serviceDays + extraDaysPrice (null sem termino da obra); "
                    + "adjustments = ajustes por data, por data; projections = "
                    + "DAY (1), WEEK (5), FORTNIGHT (10) e MONTH (22 dias uteis) com lunchMeals, dinnerMeals, "
                    + "deliveries, sodas e price = dailyPrice x dias.")
    @PutMapping("/{id}")
    public ResponseEntity<MealResponseDTO> update(@PathVariable UUID id, @RequestBody MealRequestDTO dto) {
        return ResponseEntity.ok(mealUseCase.update(id, dto));
    }

    @Operation(summary = "Exclui a refeicao", description = "Os ajustes por data da refeicao sao excluidos junto.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        mealUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Fechamento semanal da refeicao", description = "Semanas de segunda a domingo com algum dia no periodo da obra (start e end cortados no periodo; "
                    + "partial quando cortada; current quando contem hoje, em UTC); sem termino, ate a semana atual. "
                    + "week: qualquer data da semana; ausente ou fora da lista usa a semana atual, a primeira (antes "
                    + "do inicio) ou a ultima (depois do termino), informada em selected. days: os 7 dias com status "
                    + "SERVICE, NO_SERVICE, EXTRA_DAY, OFF (fim de semana sem ajuste) ou OUT_OF_PERIOD, quantidades, "
                    + "price e adjustmentIds. totals.serviceDays conta SERVICE e EXTRA_DAY." + " 404 meal.not_found.")
    @GetMapping("/{id}/weekly-closing")
    public ResponseEntity<WeeklyClosingResponseDTO> weeklyClosing(
            @PathVariable UUID id,
            @Parameter(description = "Qualquer data da semana (aaaa-mm-dd); opcional")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return ResponseEntity.ok(mealUseCase.weeklyClosing(id, week));
    }

    @Operation(summary = "Lista os ajustes por data da refeicao",
            description = "Ordenados por data. Cada ajuste traz weekday (MONDAY a SUNDAY), deliveries, sodas e "
                    + "price calculados pela refeicao atual (zero em NO_SERVICE) e inPeriod (falso quando a data "
                    + "ficou fora do periodo atual da obra; o ajuste e mantido, mas nao entra no total). "
                    + "404 meal.not_found.")
    @GetMapping("/{id}/adjustments")
    public ResponseEntity<List<MealAdjustmentResponseDTO>> findAdjustments(@PathVariable UUID id) {
        return ResponseEntity.ok(mealUseCase.findAdjustments(id));
    }

    @Operation(summary = "Inclui um ajuste por data na refeicao",
            description = ADJUSTMENT_RULES)
    @PostMapping("/{id}/adjustments")
    public ResponseEntity<MealAdjustmentResponseDTO> addAdjustment(@PathVariable UUID id,
                                                                   @RequestBody MealAdjustmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealUseCase.addAdjustment(id, dto));
    }

    @Operation(summary = "Remove um ajuste por data da refeicao",
            description = "204 sem corpo. 404 meal.not_found ou meal.adjustment.not_found (ajuste inexistente ou "
                    + "de outra refeicao). O total estimado volta a desconsiderar o ajuste.")
    @DeleteMapping("/{id}/adjustments/{adjustmentId}")
    public ResponseEntity<Void> removeAdjustment(@PathVariable UUID id, @PathVariable UUID adjustmentId) {
        mealUseCase.removeAdjustment(id, adjustmentId);
        return ResponseEntity.noContent().build();
    }
}
