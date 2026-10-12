## Context

Contrato definido pelo frontend: `App-adm-obras-front/openspec/changes/fechamento-semanal-refeicoes/design.md` (D1 e D2) e spec `fechamento-refeicoes` daquela change. Os ajustes por data (V7) e suas regras em `Meal.addAdjustment` já existem. Os use cases não dependem do Spring.

## Goals / Non-Goals

**Goals:** regras de semana e de dia no core; ajuste do restaurante atômico; soma das semanas igual ao total estimado.

**Non-Goals:** ver proposal.

## Decisions

### D1. Core
- `ClosingWeek(start, end, partial, current)` com `ClosingWeek.of(projectStart, projectEnd, today)` (lista), `defaultOf(weeks, today)` e `select(weeks, requested, today)`. As semanas são de segunda a domingo; `start` e `end` são cortados no período da obra; sem término, a lista vai até a semana de hoje e nunca termina antes da semana do início.
- `Meal.dayClosing(date)` devolve `DayClosing(date, status, partialNoService, lunchMeals, dinnerMeals, deliveries, sodas, price, adjustmentIds)`. Prioridade: fora do período → `OUT_OF_PERIOD`; ajuste na data → `NO_SERVICE` (zerado) ou `EXTRA_DAY` (números do ajuste); sábado ou domingo → `OFF`; senão `SERVICE` com os números diários.
- `WeekClosing.of(week, meals)` soma dia a dia as refeições. Situação no restaurante: `EXTRA_DAY` se alguma refeição tem dia extra; `NO_SERVICE` se todas estão sem fornecimento; `SERVICE` se alguma fornece (com `partialNoService` quando outra está sem fornecimento); senão `OFF` ou `OUT_OF_PERIOD`. Sem refeições, a situação vem do calendário e os números são zero.
- **Desvio do design do front (D1):** `getTotalPrice()` mantém a fórmula atual (custo por dia × dias de fornecimento + dias extras), que é algebricamente a soma de `dayClosing` no período. Um teste garante que a soma das semanas é igual ao total. Evita reescrever um cálculo já coberto por testes.

### D2. Aplicação e API
- `GET .../weekly-closing?week=aaaa-mm-dd`: qualquer data da semana; ausente, inválida ou fora da lista usa a semana padrão. `today` vem do `Clock` (UTC, como o resto da aplicação).
- `POST /api/restaurants/{id}/adjustments` `{ date, type, note, quantities: [{ mealId, lunchQuantity, dinnerQuantity }] }` → 201 com os ajustes criados. `NO_SERVICE` em todas as refeições; `EXTRA_DAY` só nas refeições com quantidade maior que zero (nenhuma → `meal.adjustment.invalid.quantity`); `mealId` de outra refeição → 404. Todas as regras rodam em memória antes de gravar; a gravação usa `MealRepository.updateAll`, transacional no adaptador.
- `DELETE /api/restaurants/{id}/adjustments/{date}` → 204; sem ajuste na data → 404 `meal.adjustment.not_found` (`ResourceNotFoundException.restaurantAdjustment`).
- Restaurante sem refeições: `POST` responde 400 `meal.adjustment.invalid.quantity` (não há onde gravar).

## Risks / Trade-offs

- [Data de hoje em UTC] → após 21h no horário de Brasília a "semana atual" pode virar no domingo à noite; aceitável e consistente com a auditoria.
