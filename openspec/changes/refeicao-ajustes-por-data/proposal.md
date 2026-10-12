## Why

O total estimado da refeição conta todos os dias de segunda a sexta do período da obra, mas na prática há exceções: feriado em dia útil em que o restaurante não fornece, e sábado ou domingo trabalhado em que ele fornece, muitas vezes com outra quantidade de marmitas. Sem registrar essas exceções, o total difere do que será cobrado. O frontend (change `refeicao-ajustes-por-data`, design D1 e D2) já definiu o contrato e depende desta change.

## What Changes

### Ajustes por data na refeição (desde o domínio)

- A refeição passa a agregar ajustes por data, de dois tipos, com observação opcional de até 100 caracteres:
  - `EXTRA_DAY` (dia extra): sábado ou domingo em que o restaurante fornece, com as quantidades de almoço e jantar do dia (0 a 10.000, só das refeições servidas pelo tipo, ao menos uma maior que zero);
  - `NO_SERVICE` (sem fornecimento): dia útil em que o restaurante não fornece, sem quantidades.
- Regras, nesta ordem, com 400 salvo indicação: data dentro do período da obra (`meal.adjustment.invalid.date`); data não repetida na refeição (409 `meal.adjustment.duplicate.date`); `EXTRA_DAY` só sábado ou domingo (`meal.adjustment.invalid.extra_day`); `NO_SERVICE` só de segunda a sexta (`meal.adjustment.invalid.no_service`); quantidades (`meal.adjustment.invalid.quantity`).
- Valor do dia extra: marmitas das refeições com quantidade maior que zero, uma entrega por refeição com quantidade maior que zero à taxa do restaurante, e um refrigerante por entrega quando o restaurante fornece.

### Total estimado com ajustes

- **BREAKING** (valor): `totalPrice` = `dailyPrice × serviceDays + extraDaysPrice`, com `serviceDays` = `workingDays − noServiceDays`. Sem ajustes, o valor não muda. Sem término da obra, continua `null`. O total da obra acompanha.
- Só ajustes dentro do período atual da obra entram no cálculo. Ajuste que fica fora após mudar o término é mantido, com `inPeriod` falso.

### API

- `GET /api/meals/{id}/adjustments`, `POST /api/meals/{id}/adjustments` (201) e `DELETE /api/meals/{id}/adjustments/{adjId}` (204). Escrita só para ADMIN.
- A refeição resumida ganha `serviceDays`, `noServiceDays`, `extraDays` e `extraDaysPrice`; o detalhe da refeição ganha `adjustments`, ordenados por data.

### Persistência

- Migração `V7__ajustes_por_data_da_refeicao.sql`: tabela `meal_adjustment` com auditoria, FK para `meal` com `on delete cascade` e `unique (meal_id, date)`.

### Não-objetivos

- Calendário de feriados compartilhado entre obras.
- Ajuste de quantidade em dia útil normal.
- Ajustes na projeção fixa por período (`DAY`, `WEEK`, `FORTNIGHT`, `MONTH`).

## Capabilities

### New Capabilities

- `meal-date-adjustments`: ajustes por data da refeição (dia extra e sem fornecimento), suas regras, o valor do dia extra e o total estimado com ajustes.

### Modified Capabilities

(nenhuma; `openspec/specs/` ainda está vazio. O requisito "Estimated total with adjustments" desta change substitui a fórmula de `totalPrice` dos requisitos "Working days and estimated total" (`project-meal-plan`) e "Daily price breakdown" (`meal-cost-breakdown`). Ao arquivar, esta change vem depois de `refeicoes-por-obra-e-email-opcional`, `restaurante-entrega-refrigerante-periodos` e `refrigerante-no-restaurante`.)

## Impact

**Código**
- `core/domain`: `MealAdjustment` (novo, com o tipo), `Meal` (agregado de ajustes, dias de fornecimento, dias extras, total).
- `core/exception`: `InvalidMealAdjustmentException` (nova).
- `application`: `MealAdjustmentRequestDTO`, `MealAdjustmentResponseDTO`, campos novos em `MealSummaryDTO`/`MealResponseDTO`, `MealAdjustmentMapper`, `MealUseCase` (incluir, listar e remover), `ResourceNotFoundException.mealAdjustment`.
- `infra`: `MealAdjustmentEntity`, `MealEntity` (coleção), `MealEntityMapper`, `MealController` (endpoints aninhados e OpenAPI), `IntegrityViolationTranslator` (`uk_meal_adjustment_meal_date`).
- `src/main/resources/db/migration/V7__ajustes_por_data_da_refeicao.sql`.

**API**
- Aditiva nos campos e endpoints. **BREAKING** no valor de `totalPrice` só para refeições com ajustes.

**Documentação**
- `README.md` e `app-adm-obras-erd.puml` (`meal_adjustment`).

**Integração**
- O frontend publica junto. Rollback: reverter o código; a tabela `meal_adjustment` pode ficar sem uso.
