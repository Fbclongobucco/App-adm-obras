# Tasks

Referência do contrato: `App-adm-obras-front/openspec/changes/melhorias-design-refeicoes-cadastros/design.md` (D1, D2, D7).

## 1. Domínio da refeição e da obra

- [x] 1.1 `MealType` com `LUNCH_AND_DINNER`, `servesLunch()`/`servesDinner()`; `Meal` sem `date`/`quantity`/`price`, com `configure(...)` (design D2), `getDailyPrice()`, `getWorkingDays()`, `getTotalPrice()` e `overlaps(...)`; `InvalidMealException` com `meal.invalid.lunch_quantity`, `lunch_price`, `dinner_quantity`, `dinner_price` e sem os códigos de preço, quantidade e data. Verificar por `MealTest`: exemplo 10 × 25,00 + 8 × 22,00 em obra de 2026-10-05 a 2026-10-16 → 426,00, 10 dias, 4260,00; obra sem término → `null`; campos ignorados do tipo não servido; cada código de erro; sobreposição por tipo.
- [x] 1.2 `Project` com coleção de refeições (`addMeal` liga a obra e rejeita sobreposição), `getWorkingDays()` e `getTotalPrice()` calculado (design D3); remover `lunch`/`dinner`, `refreshTotalPrice()` e a chamada em `Accommodation`. Verificar por `ProjectTest`: dias úteis com fim de semana, início e término no sábado, mesmo dia; total da obra com refeição sem término; alteração do término muda o total.

## 2. E-mail opcional no domínio

- [x] 2.1 `Client` e `Employee`: e-mail `null`/em branco vira `null`; informado segue validado (design D6). Verificar por `ClientTest` e `EmployeeTest`.
- [x] 2.2 `ClientUseCase` e `EmployeeUseCase` só checam duplicidade com e-mail não nulo. Verificar por testes de caso de uso: dois registros sem e-mail não consultam duplicidade; e-mail repetido continua 409.

## 3. Persistência e migração

- [x] 3.1 Criar `V4__refeicoes_por_obra_e_email_opcional.sql` (design D7) e ajustar `MealEntity`, `ClientEntity`, `EmployeeEntity`, `MealEntityMapper` (grava `lunch_slot`/`dinner_slot`) e `MealJpaRepository`/`MealRepository` sem consultas por data. Verificar por `MealMigrationTest`: base no `V3` com `LUNCH` e `DINNER` migra para os campos novos; e por teste que insere duas coberturas de almoço por SQL e recebe violação.
- [x] 3.2 `ProjectRepositoryAdapter`/`ProjectEntityMapper` carregam todas as refeições da obra. `IntegrityViolationTranslator` mapeia `uk_meal_project_lunch`/`uk_meal_project_dinner` para `meal.duplicate.type`. Verificar por `ProjectEntityMapperTest` e `IntegrityViolationTranslatorTest`.

## 4. Aplicação e API

- [x] 4.1 `MealRequestDTO`, `MealSummaryDTO`, `MealResponseDTO` e `MealMapper` no contrato D2 do front; `MealUseCase` checa sobreposição com `Meal.overlaps`. Verificar por `MealMapperTest` e `MealUseCaseTest` (409 com almoço sobre almoço e jantar, aceito jantar após almoço, edição da própria refeição).
- [x] 4.2 `ProjectRequestDTO` sem `lunchId`/`dinnerId`; `ProjectResponseDTO` com `meals`; `ProjectUseCase.update` mantém as refeições da obra. Verificar por `ProjectMapperTest` e `ProjectUseCaseTest`.
- [x] 4.3 Testes de API: refeição `LUNCH_AND_DINNER` com custos calculados, 409 `meal.duplicate.type`, 400 por campo, obra com `meals`, PUT da obra muda término e refeição recalcula; cliente e colaborador sem e-mail (dois de cada) e e-mail inválido. Verificar com `./mvnw test`.
- [x] 4.4 Documentação OpenAPI dos controllers de refeição e obra, `README.md` (contrato e **BREAKING**) e `app-adm-obras-erd.puml` (colunas de `meal`, e-mail anulável). Verificar por `OpenApiDocumentationIntegrationTest` e leitura do `.puml` contra a `V4`.

## 5. Integração

- [x] 5.1 `./mvnw test` sem falhas e `openspec validate refeicoes-por-obra-e-email-opcional --strict` válido.

## Workflow follow-up

- Aplicar a `V4` num PostgreSQL antes do deploy e conferir as refeições migradas (design, Migration Plan).
- Regerar `app-adm-obras-erd.pdf` com `./mvnw -Pdiagrams` (exige Graphviz, ausente neste ambiente; o `.puml` já está atualizado).
- Atualizar `scripts/seed.mjs`, `types.ts` e `errors.ts` no front (change `melhorias-design-refeicoes-cadastros`).
