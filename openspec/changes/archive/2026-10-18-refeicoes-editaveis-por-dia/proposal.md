## Why

O fechamento semanal usa a mesma quantidade em todo dia útil e só aceita feriado e dia extra. Na prática, há dia com uma janta a mais ou a menos, a quantidade de entregas do almoço e do jantar varia, e a taxa de entrega do almoço pode ser diferente da do jantar. O frontend (change `refeicoes-editaveis-por-dia`, design D1 a D6) definiu o contrato e depende desta change.

## What Changes

- **BREAKING (contrato do restaurante):** `deliveryFee` é substituído por `lunchDeliveryFee` e `dinnerDeliveryFee`, opcionais e maiores que zero. A V10 copia a taxa atual para as duas.
- Custo por dia com a taxa de cada refeição servida.
- Ajuste por data `CHANGED` (dia alterado), só de segunda a sexta, com quantidades das refeições servidas.
- `lunchDeliveries`, `dinnerDeliveries` e `sodas` opcionais em `EXTRA_DAY` e `CHANGED`; vazio = padrão (1 entrega por refeição com quantidade, 1 refrigerante por entrega quando o restaurante fornece).
- Total estimado como soma do fechamento de cada dia do período.
- Fechamento: situação `CHANGED`, `lunchDeliveries`, `dinnerDeliveries`, `note` e, no restaurante, os números de cada refeição no dia.
- `PUT /api/meals/{id}/adjustments/{date}` e `PUT /api/restaurants/{id}/adjustments/{date}`: grava o dia (cria ou substitui), atômico no restaurante.

## Capabilities

### New Capabilities

- `meal-day-changes`: taxa de entrega por refeição, dia alterado, entregas e refrigerantes do dia e gravação do dia por data.

### Modified Capabilities

<!-- Nenhuma em openspec/specs. Estende o fechamento de `fechamento-semanal-refeicoes` e os ajustes de `refeicao-ajustes-por-data`, ainda não arquivadas. -->

## Impact

- Core: `Restaurant`, `Meal`, `MealAdjustment`, `DayClosing`, `WeekClosing`, `InvalidMealAdjustmentException`.
- Aplicação: DTOs do restaurante, do ajuste e do fechamento; `MealUseCase`, `RestaurantUseCase`, mappers.
- Infra: V10, `RestaurantEntity`, `MealAdjustmentEntity`, mappers de entidade, controllers e OpenAPI.
- Testes de domínio, de mapper, de use case, de integração e de migração.
