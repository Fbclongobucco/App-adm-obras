# Tasks

Referência do contrato: `App-adm-obras-front/openspec/changes/fechamento-semanal-refeicoes/design.md` (D1 e D2).

## 1. Core

- [x] 1.1 `ClosingWeek`, `DayClosing`, `WeekClosing` e `Meal.dayClosing`. Verificar por `WeeklyClosingTest`: semanas completas e parciais, obra sem término, semana padrão (atual, antes do início, depois do término), seleção por data, R$ 2.423,00 + R$ 1.808,00 = R$ 4.231,00 = `totalPrice`, restaurante com refeições separadas (R$ 452,00 por dia) e situação parcial.

## 2. Aplicação e API

- [x] 2.1 DTOs, `MealUseCase.weeklyClosing`, `RestaurantUseCase.weeklyClosing`/`addAdjustment`/`removeAdjustments`, `MealRepository.updateAll` e endpoints. Verificar por `WeeklyClosingIntegrationTest`: GET das duas rotas com `week` e sem, feriado nas 2 refeições, conflito 409 sem gravar nada, dia extra só com quantidade, DELETE 204 e 404, 403 para OPERADOR.
- [x] 2.2 README e OpenAPI. Verificar no README e em `OpenApiDocumentationIntegrationTest`.

## 3. Integração

- [x] 3.1 `./mvnw test` sem falhas e `openspec validate fechamento-semanal-refeicoes --strict` válido.
