## Why

O fechamento das marmitas com o restaurante é semanal, de segunda a domingo, mas a API só oferece a projeção fixa ("semana = 5 dias úteis"), sem datas reais nem ajustes. O frontend (change `fechamento-semanal-refeicoes`, design D1 e D2) definiu o contrato e depende desta change.

## What Changes

- Semanas de fechamento da obra (segunda a domingo), cortadas no início e no término; sem término, até a semana atual. Semana padrão: a atual, a primeira (antes do início) ou a última (depois do término).
- Fechamento diário da refeição: situação (`SERVICE`, `NO_SERVICE`, `EXTRA_DAY`, `OFF`, `OUT_OF_PERIOD`), almoços, jantares, entregas, refrigerantes e valor, com os ajustes por data.
- Fechamento da semana por refeição e por restaurante (soma das refeições), com totais.
- `GET /api/meals/{id}/weekly-closing?week=` e `GET /api/restaurants/{id}/weekly-closing?week=`.
- `POST /api/restaurants/{id}/adjustments` e `DELETE /api/restaurants/{id}/adjustments/{date}`: feriado ou dia extra em todas as refeições do restaurante, de forma atômica.

### Não-objetivos

- Registro de fechamento efetivado, dia de corte configurável e exportação.

## Capabilities

### New Capabilities

- `weekly-meal-closing`: semanas de fechamento, fechamento diário e semanal por refeição e restaurante, e ajustes por data no restaurante.

### Modified Capabilities

<!-- Nenhuma. Usa os ajustes de `meal-date-adjustments` (change refeicao-ajustes-por-data) sem mudar suas regras. -->

## Impact

- Core: `ClosingWeek`, `DayClosing`, `WeekClosing`, `Meal.dayClosing`; porta `MealRepository.updateAll`.
- Aplicação: DTOs do fechamento e dos ajustes do restaurante, `MealUseCase` e `RestaurantUseCase`.
- Infra: endpoints, `updateAll` transacional. Sem migração.
