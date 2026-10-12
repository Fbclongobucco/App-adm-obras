# Design

## Context

- Contrato definido pelo frontend em `App-adm-obras-front/openspec/changes/restaurante-custos-periodos/design.md` (D1 e D2). Esta change o implementa literalmente. Motivação: ver `proposal.md`.
- `Meal` já calcula `getDailyPrice()`, `getWorkingDays()` e `getTotalPrice()` na leitura (change `refeicoes-por-obra-e-email-opcional`, D3). `MealType` sabe se serve almoço e jantar. `Project.getTotalPrice()` soma o `totalPrice` das refeições.
- `Restaurant` tem nome, telefone, endereço e obra. `RestaurantResponseDTO` traz `project`, sem as refeições. O domínio não tem relação do restaurante para as refeições; a refeição aponta para o restaurante.
- Bancos: PostgreSQL em `prod`/compose; H2 2.x com `MODE=PostgreSQL` nos perfis `local` e `test`. A última migração é a `V4`.

## Goals / Non-Goals

**Goals:**
- Regra de entregas, refrigerante, parcelas do custo e projeção só no domínio (`core/domain`). DTO, mapper, entidade e migração acompanham.
- Valores atuais preservados enquanto taxa e refrigerante estiverem vazios.
- Listagem de restaurantes sem N+1 consultas de refeições.

**Non-Goals:**
- Relação bidirecional restaurante → refeições no domínio ou na entidade JPA.
- Gravar parcelas ou projeções.

## Decisions

### D1. Taxa no restaurante, refrigerante na refeição
- `Restaurant.deliveryFee` (`BigDecimal`, anulável). `setDeliveryFee` aceita `null` e rejeita zero ou negativo com `InvalidRestaurantException.invalidDeliveryFee` (`restaurant.invalid.delivery_fee`).
- `Meal.sodaPrice` (`BigDecimal`, anulável), mesma regra (`InvalidMealException.invalidSodaPrice`, `meal.invalid.soda_price`). Fica fora de `configure(...)`: não depende do tipo.
- Os construtores ganham uma sobrecarga com o campo novo; a assinatura anterior delega com `null`, para não reescrever chamadas que não usam taxa nem refrigerante.
- **Alternativa:** preço do refrigerante no restaurante. Descartada pelo front (D1): o fornecimento do refrigerante é decisão por refeição.

### D2. Entregas e parcelas do custo
- `MealType.deliveriesPerDay()`: conta as refeições servidas (`LUNCH`/`DINNER` = 1, `LUNCH_AND_DINNER` = 2).
- `Meal.getMealsDailyPrice()` é o cálculo atual. `getDeliveryDailyPrice()` = entregas × taxa do restaurante (zero sem taxa). `getSodaDailyPrice()` = entregas × `sodaPrice` (zero sem refrigerante). `getDailyPrice()` soma as três. `getTotalPrice()` não muda de fórmula e passa a incluir entrega e refrigerante; `Project.getTotalPrice()` acompanha sem alteração.
- A taxa é lida do restaurante em cada chamada: alterar a taxa reflete na leitura seguinte das refeições.

### D3. Projeção por período
- `enum BillingPeriod { DAY(1), WEEK(5), FORTNIGHT(10), MONTH(22) }` em `core/domain`, com `getWorkingDays()`.
- `record MealProjection(period, workingDays, lunchMeals, dinnerMeals, deliveries, sodas, price)` em `core/domain`, criado por `Meal.projection(BillingPeriod)`: `lunchMeals` = `lunchQuantity × dias` (0 se não serve), idem jantar; `deliveries` = `deliveriesPerDay × dias`; `sodas` = `deliveries` com refrigerante, senão 0; `price` = `dailyPrice × dias`.
- `Meal.getProjections()` devolve os quatro períodos na ordem do enum. O mapper só converte para `MealProjectionDTO`.
- **Alternativa:** calcular no front. Descartada: o usuário pediu a regra no core, e o fechamento precisa bater com relatórios futuros.

### D4. Restaurante com refeições, numa consulta por página
- `MealRepository.findByRestaurantIn(Collection<Restaurant>)`; o adaptador chama `MealJpaRepository.findByRestaurantIdIn(ids)` ordenado por `createdAt, id`, e devolve lista vazia sem consultar quando não há restaurantes.
- `RestaurantUseCase` agrupa as refeições por id do restaurante e monta `RestaurantMapper.toResponse(restaurant, meals)`. Vale para criação (lista vazia), alteração, detalhe, listagem paginada e listagem por obra.
- `RestaurantResponseDTO` ganha `meals: List<MealSummaryDTO>`; `RestaurantSummaryDTO` ganha `deliveryFee`. A refeição resumida continua trazendo `restaurant` (resumo), conforme o contrato.
- **Alternativa:** `@OneToMany` em `RestaurantEntity` ou lista no domínio `Restaurant`. Descartada: cria ciclo restaurante ↔ refeição no domínio e nos mappers, e a leitura da obra já carrega as refeições por outro caminho.

### D5. Migração `V5__taxa_entrega_e_refrigerante.sql`
`alter table restaurant add column delivery_fee numeric(19, 2)` e `alter table meal add column soda_price numeric(19, 2)`. Sem `default` nem `update`: os registros existentes ficam `null`, sem taxa e sem refrigerante. SQL comum a PostgreSQL e H2.

### D6. Substitui o requisito "Daily price" de `project-meal-plan`
`project-meal-plan` ainda não foi arquivada (`openspec/specs/` vazio), então a mudança de significado de `dailyPrice` é registrada como requisito novo em `meal-cost-breakdown`. Ao arquivar, esta change vem depois de `refeicoes-por-obra-e-email-opcional`, e o requisito "Daily price breakdown" prevalece sobre "Daily price".

## Risks / Trade-offs

- [`dailyPrice` muda de significado] → só muda quando taxa ou refrigerante são preenchidos; README e OpenAPI registram a nova composição, e a resposta expõe as parcelas.
- [Mês fixo de 22 dias úteis difere do mês real] → o período traz `workingDays`; o total até o término continua usando os dias úteis reais da obra.
- [Resposta do restaurante maior, com refeições e projeções] → no máximo duas refeições por restaurante na prática (uma cobertura de almoço e uma de jantar por obra); uma consulta adicional por página.
- [V5 testada só em H2 nos testes automatizados] → SQL restrito a `add column` anulável. Aplicar numa cópia da base PostgreSQL antes do deploy.

## Migration Plan

1. Deploy do backend: Flyway aplica a `V5` no boot. Dados existentes ficam sem taxa e sem refrigerante.
2. Deploy do front com os campos novos (aditivos).
3. Rollback: reverter o código basta; a `V5` pode ficar (colunas anuláveis ignoradas pelo código anterior).
