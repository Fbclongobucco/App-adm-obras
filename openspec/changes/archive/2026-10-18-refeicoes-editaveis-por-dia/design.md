## Context

Contrato definido no design do frontend (`App-adm-obras-front/openspec/changes/refeicoes-editaveis-por-dia/design.md`, D1 a D5). Este documento registra só as decisões próprias do backend.

## Decisions

- **Restaurante:** construtores de 6 e 7 argumentos mantidos, com a taxa única aplicada às duas refeições (compatibilidade dos testes e fixtures). Novo construtor de 8 argumentos com as duas taxas. `restaurant.invalid.delivery_fee` vale para as duas.
- **Padrões efetivos** em `MealAdjustment`: `lunchDeliveries(meal)`, `dinnerDeliveries(meal)`, `deliveries(meal)` (soma) e `sodas(meal)`. `price(meal)` usa a taxa de cada refeição.
- **Validação** em `Meal`, compartilhada por `addAdjustment` e `putAdjustment`, nesta ordem: período, data repetida (só no add), tipo × dia da semana, quantidades, entregas, refrigerantes. Códigos novos, na convenção atual:
  - `meal.adjustment.invalid.changed_day`;
  - `meal.adjustment.invalid.deliveries` (fora de 0 a 100);
  - `meal.adjustment.invalid.delivery_without_meal`;
  - `meal.adjustment.invalid.sodas` (fora de 0 a 10.000);
  - `meal.adjustment.invalid.soda_not_supplied`.
- **`putAdjustment`:** substitui o ajuste da mesma data mantendo id e auditoria de criação; senão inclui. Aceita os três tipos.
- **Total:** `getTotalPrice()` soma `dayClosing` do início ao término. `getServiceDays()` continua como dias úteis − sem fornecimento (usado na tela); novo `getChangedDays()`.
- **Restaurante, PUT:** `mealId` de outra refeição → 404 `meal.not_found`, como o POST atual. `CHANGED` com todas as quantidades 0 numa refeição → `NO_SERVICE`; números iguais ao padrão → remove o ajuste da data; nenhuma refeição com quantidade → 400 `meal.adjustment.invalid.quantity`. Gravação única por `updateAll`, como o POST.
- **Fechamento do restaurante:** cada dia traz `meals` com os números de cada refeição; na refeição, `meals` vem vazio.

## Risks / Trade-offs

- [Contrato do restaurante muda] → front e backend publicados juntos.
- [Mudança de fórmula do total] → testes atuais de total servem de regressão.
