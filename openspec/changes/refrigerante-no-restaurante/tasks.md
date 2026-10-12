# Tasks

Referência do contrato: `App-adm-obras-front/openspec/changes/refrigerante-no-restaurante/design.md` (D1, D2 e D3).

## 1. Domínio

- [x] 1.1 `Restaurant.sodaPrice` com validação e `InvalidRestaurantException.invalidSodaPrice` (`restaurant.invalid.soda_price`) (design D1). Verificar por `RestaurantTest`: `null` aceito, 5.00 aceito, 0 e -1 rejeitados com o código, valor anterior mantido após rejeição.
- [x] 1.2 `Meal` sem atributo nem setter de `sodaPrice`; `getSodaPrice()` delega ao restaurante; remover `InvalidMealException.invalidSodaPrice` (design D1). Verificar por `MealTest`: 452,00/dia com refrigerante do restaurante; alterar o preço do restaurante muda `sodaDailyPrice`, `dailyPrice` e `projections` das refeições dele; restaurante sem refrigerante → `sodas` 0.

## 2. Persistência e migração

- [x] 2.1 Criar `V6__refrigerante_no_restaurante.sql` (design D3), mapear `soda_price` em `RestaurantEntity`/`RestaurantEntityMapper` e removê-lo de `MealEntity`/`MealEntityMapper`. Verificar por `SchemaMigrationIntegrationTest` (`ddl-auto=validate`) e por `SodaPriceMigrationTest`: base no `V5` com restaurante de preços 5.00 e 6.00 → 6.00; restaurante misto (5.00 e sem) → 5.00; restaurante sem refrigerante → `null`; `meal.soda_price` não existe mais. `CostColumnsMigrationTest` migra até a versão 5.

## 3. Aplicação e API

- [x] 3.1 `RestaurantRequestDTO`, `RestaurantSummaryDTO` e `RestaurantResponseDTO` com `sodaPrice`; `MealRequestDTO` sem `sodaPrice`; `RestaurantMapper` e `MealMapper` (design D2). Verificar por `RestaurantMapperTest` e `MealMapperTest`.
- [x] 3.2 Testes de API: restaurante com refrigerante (201 e leitura), refrigerante inválido (400 `restaurant.invalid.soda_price`), refeição criada com `sodaPrice` no corpo ignora o campo, refeição lê o refrigerante do restaurante (452,00/dia), alteração do refrigerante do restaurante refletida nas refeições e no total da obra. Verificar com `./mvnw test`.
- [x] 3.3 Documentação OpenAPI dos controllers de restaurante e refeição, `README.md` (contrato e nota **BREAKING** na escrita de refeição) e `app-adm-obras-erd.puml` (`soda_price` em `restaurant`, fora de `meal`). Verificar por `OpenApiDocumentationIntegrationTest` e leitura do `.puml` contra a `V6`.

## 4. Integração

- [x] 4.1 `./mvnw test` sem falhas e `openspec validate refrigerante-no-restaurante --strict` válido.

## Workflow follow-up

- Rodar a consulta de restaurantes mistos (design, Risks) numa cópia da base PostgreSQL antes do deploy.
- Regerar `app-adm-obras-erd.pdf` com `./mvnw -Pdiagrams` (exige Graphviz).
- Arquivar depois de `restaurante-entrega-refrigerante-periodos`.
