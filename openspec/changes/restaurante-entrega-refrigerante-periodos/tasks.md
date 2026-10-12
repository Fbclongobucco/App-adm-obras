# Tasks

Referência do contrato: `App-adm-obras-front/openspec/changes/restaurante-custos-periodos/design.md` (D1 e D2).

## 1. Domínio

- [x] 1.1 `Restaurant.deliveryFee` com validação e `InvalidRestaurantException.invalidDeliveryFee` (`restaurant.invalid.delivery_fee`) (design D1). Verificar por `RestaurantTest`: `null` aceito, 8.00 aceito, 0 e -1 rejeitados com o código.
- [x] 1.2 `Meal.sodaPrice` com `InvalidMealException.invalidSodaPrice` (`meal.invalid.soda_price`), `MealType.deliveriesPerDay()`, `getMealsDailyPrice()`, `getDeliveryDailyPrice()`, `getSodaDailyPrice()` e `getDailyPrice()` como soma (design D1 e D2). Verificar por `MealTest`: 10 × 25,00 + 8 × 22,00 com taxa 8,00 e refrigerante 5,00 → 426,00 / 16,00 / 10,00 / 452,00; só almoço com taxa → 258,00, 1 entrega, refrigerante 0; sem taxa e sem refrigerante → 426,00; `sodaPrice` 0 e -1 rejeitados; total 4.520,00 na obra de 10 dias úteis.
- [x] 1.3 `BillingPeriod`, `MealProjection`, `Meal.projection(...)` e `Meal.getProjections()` (design D3). Verificar por `MealTest`: DAY 452,00; WEEK 2.260,00 (50/40/10/10); FORTNIGHT 4.520,00; MONTH 9.944,00 (220/176/44/44); só almoço sem refrigerante → jantares 0 e refrigerantes 0; ordem DAY, WEEK, FORTNIGHT, MONTH.

## 2. Persistência e migração

- [x] 2.1 Criar `V5__taxa_entrega_e_refrigerante.sql` (design D5) e mapear `delivery_fee` em `RestaurantEntity`/`RestaurantEntityMapper` e `soda_price` em `MealEntity`/`MealEntityMapper`. Verificar por `SchemaMigrationIntegrationTest` (versão 5, `ddl-auto=validate`) e por `CostColumnsMigrationTest`: base no `V4` com restaurante e refeição migra com as colunas novas `null` e anuláveis.
- [x] 2.2 `MealRepository.findByRestaurantIn` com `MealJpaRepository.findByRestaurantIdIn` numa consulta (design D4). Verificar pelos testes de integração de restaurante (item 3.3).

## 3. Aplicação e API

- [x] 3.1 `RestaurantRequestDTO`, `RestaurantSummaryDTO` e `RestaurantResponseDTO` (`deliveryFee`, `meals`), `RestaurantMapper` e `RestaurantUseCase` carregando as refeições por página (design D4). Verificar por `RestaurantMapperTest` e `RestaurantUseCaseTest` (listagem faz uma única chamada a `findByRestaurantIn`, agrupa por restaurante, criação traz `meals` vazio).
- [x] 3.2 `MealRequestDTO` (`sodaPrice`), `MealSummaryDTO`/`MealResponseDTO` (`sodaPrice`, `deliveriesPerDay`, parcelas, `projections`), `MealProjectionDTO` e `MealMapper`. Verificar por `MealMapperTest`: parcelas, `restaurant.deliveryFee` e quatro projeções na ordem.
- [x] 3.3 Testes de API: restaurante com taxa e com taxa inválida (400), `GET /api/restaurants` e `/{id}` com `meals`, refeição com refrigerante e parcelas, `projections`, refrigerante inválido (400), alteração da taxa refletida na refeição e total da obra com entrega e refrigerante. Verificar com `./mvnw test`.
- [x] 3.4 Documentação OpenAPI dos controllers de restaurante e refeição, `README.md` (contrato e nota de que `dailyPrice` inclui entrega e refrigerante) e `app-adm-obras-erd.puml` (`delivery_fee`, `soda_price`). Verificar por `OpenApiDocumentationIntegrationTest` e leitura do `.puml` contra a `V5`.

## 4. Integração

- [x] 4.1 `./mvnw test` sem falhas e `openspec validate restaurante-entrega-refrigerante-periodos --strict` válido.

## Workflow follow-up

- Aplicar a `V5` numa cópia da base PostgreSQL antes do deploy.
- Regerar `app-adm-obras-erd.pdf` com `./mvnw -Pdiagrams` (exige Graphviz).
- Arquivar depois de `refeicoes-por-obra-e-email-opcional` (design D6).
