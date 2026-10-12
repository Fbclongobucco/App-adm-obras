# Tasks

Referência do contrato: `App-adm-obras-front/openspec/changes/refeicao-ajustes-por-data/design.md` (D1 e D2) e spec `refeicoes-obra` daquela change.

## 1. Domínio

- [x] 1.1 `MealAdjustment` (tipo, observação, `deliveries`/`sodas`/`price` a partir da refeição) e `InvalidMealAdjustmentException` (design D1). Verificar por `MealAdjustmentTest`: observação normalizada e limite de 100 (`meal.adjustment.invalid.note`), tipo obrigatório (`meal.adjustment.invalid.type`), `NO_SERVICE` com valor zero.
- [x] 1.2 `Meal.addAdjustment`/`removeAdjustment`/`restoreAdjustment`/`getAdjustments`, `isInPeriod`, `getNoServiceDays`, `getExtraDays`, `getExtraDaysPrice`, `getServiceDays` e `getTotalPrice` com ajustes (design D1). Verificar por `MealAdjustmentRulesTest`: obra 2026-10-05 a 2026-10-16, `LUNCH_AND_DINNER` 10 × 25,00 e 8 × 22,00, taxa 8,00, refrigerante 5,00 (452,00/dia), `NO_SERVICE` 2026-10-12 e `EXTRA_DAY` 2026-10-10 com 6 almoços e 0 jantares → extra 163,00 (1 entrega, 1 refrigerante), `workingDays` 10, `noServiceDays` 1, `serviceDays` 9, `totalPrice` 4231,00; erros na ordem do design (data fora antes de repetida, repetida antes de tipo/dia); `EXTRA_DAY` em dia útil, `NO_SERVICE` no sábado, quantidades zeradas, faltando, acima de 10000 e informadas no `NO_SERVICE`; quantidade não servida descartada; ajuste fora do período após mudar o término (`inPeriod` falso, fora do total); obra sem término; lista inalterada após erro; total da obra com ajustes.

## 2. Persistência e migração

- [x] 2.1 Criar `V7__ajustes_por_data_da_refeicao.sql`, `MealAdjustmentEntity`, coleção em `MealEntity` com `@BatchSize`, sincronização em `MealEntityMapper` e `uk_meal_adjustment_meal_date` no `IntegrityViolationTranslator` (design D3). Verificar por `SchemaMigrationIntegrationTest` (versão 7, `ddl-auto=validate`), `MealAdjustmentMigrationTest` (tabela, `unique (meal_id, date)` e `on delete cascade` em H2) e `IntegrityViolationTranslatorTest`.

## 3. Aplicação e API

- [x] 3.1 `MealAdjustmentRequestDTO`, `MealAdjustmentResponseDTO`, `MealAdjustmentMapper`, campos novos em `MealSummaryDTO`/`MealResponseDTO`, `MealMapper` levando os ajustes no `PUT`, `MealUseCase` (listar, incluir, remover) e `ResourceNotFoundException.mealAdjustment` (design D2). Verificar por `MealMapperTest` e `MealUseCaseTest` (inclusão grava pela refeição; remoção de ajuste de outra refeição → 404; `PUT` mantém os ajustes).
- [x] 3.2 Endpoints em `MealController` com OpenAPI. Verificar por `MealAdjustmentIntegrationTest`: 201 e lista, 204, 409, 400 com cada código, 404, 403 para OPERADOR, `GET /api/meals/{id}` com `adjustments` por data e campos novos, total da obra 4231,00, mudança de término marcando `inPeriod` falso, `PUT` da refeição mantendo os ajustes e exclusão da refeição em cascata.
- [x] 3.3 `README.md` (contrato, regras e códigos) e `app-adm-obras-erd.puml` (`meal_adjustment`). Verificar por `OpenApiDocumentationIntegrationTest` e leitura do `.puml` contra a `V7`.

## 4. Integração

- [x] 4.1 `./mvnw test` sem falhas e `openspec validate refeicao-ajustes-por-data --strict` válido.

## Workflow follow-up

- Aplicar a `V7` numa cópia da base PostgreSQL antes do deploy.
- Regerar `app-adm-obras-erd.pdf` com `./mvnw -Pdiagrams` (exige Graphviz).
- Arquivar depois de `refeicoes-por-obra-e-email-opcional`, `restaurante-entrega-refrigerante-periodos` e `refrigerante-no-restaurante` (design D4).
