## Context

- Contrato definido pelo frontend em `App-adm-obras-front/openspec/changes/refeicao-ajustes-por-data/design.md` (D1 e D2) e na spec `refeicoes-obra` daquela change. Esta change o implementa. Motivação: ver `proposal.md`.
- `Meal` calcula na leitura `getWorkingDays()` (via `Project.getWorkingDays()`), `getDailyPrice()` (marmitas + entrega + refrigerante do restaurante, changes `restaurante-entrega-refrigerante-periodos` e `refrigerante-no-restaurante`), `getTotalPrice()` e `projection(...)`. `Project.getTotalPrice()` soma o `totalPrice` das refeições.
- A refeição é lida por três caminhos: `MealRepositoryAdapter` (detalhe, listagens), `ProjectRepositoryAdapter` (refeições da obra) e `RestaurantUseCase` via `findByRestaurantIn`. Todos passam por `MealEntityMapper.toDomain`.
- `MealUseCase.update` monta uma `Meal` nova a partir do DTO e grava com `MealRepository.update`.
- Bancos: PostgreSQL em `prod`/compose; H2 2.x com `MODE=PostgreSQL` nos perfis `local` e `test`. A última migração é a `V6` (`refrigerante-no-restaurante`).

## Goals / Non-Goals

**Goals:**
- Regras de data, tipo, quantidades e cálculo só no domínio (`core/domain`). DTO, mapper, entidade e migração acompanham.
- Ajustes carregados junto com a refeição em todos os caminhos de leitura, para o total da refeição, do restaurante e da obra baterem.

**Non-Goals:**
- Calendário de feriados; ajuste de quantidade em dia útil; projeção por período com ajustes.
- Alteração de um ajuste existente (remove e inclui de novo).

## Decisions

### D1. Domínio
- `MealAdjustment` (`core/domain`, estende `Audited`): `id`, `date`, `type` (`MealAdjustment.Type { EXTRA_DAY, NO_SERVICE }`), `lunchQuantity`, `dinnerQuantity`, `note`. O construtor valida só o que é intrínseco: `type` obrigatório (`meal.adjustment.invalid.type`) e `note` normalizada (strip, vazia = `null`, até 100 caracteres, senão `meal.adjustment.invalid.note`).
- `Meal` agrega a lista. `addAdjustment(MealAdjustment)` é o único ponto de inclusão com regras e valida, nesta ordem:
  1. data presente e dentro do período da obra: `>= startDate` e, havendo término, `<= endDate` (`InvalidMealAdjustmentException`, `meal.adjustment.invalid.date`);
  2. data não repetida entre todos os ajustes guardados (`ConflictException`, `meal.adjustment.duplicate.date`, 409);
  3. `EXTRA_DAY` só sábado ou domingo (`meal.adjustment.invalid.extra_day`);
  4. `NO_SERVICE` só segunda a sexta (`meal.adjustment.invalid.no_service`);
  5. `EXTRA_DAY`: para cada refeição servida pelo tipo, quantidade obrigatória de 0 a 10000; a da não servida é descartada (como em `Meal.configure`); ao menos uma maior que zero (`meal.adjustment.invalid.quantity`);
  6. `NO_SERVICE`: quantidades nulas (`meal.adjustment.invalid.quantity`).
  Falhando qualquer regra, a lista não muda. O ajuste incluído é a instância validada (com a quantidade não servida descartada).
- `removeAdjustment(UUID)` devolve se removeu. `restoreAdjustment(MealAdjustment)` inclui sem regras, só para reidratar do banco e para levar os ajustes de uma refeição alterada.
- `getAdjustments()` devolve a lista ordenada por data, somente leitura.
- `MealAdjustment.deliveries(meal)` = refeições servidas pelo tipo atual com quantidade maior que zero; `sodas(meal)` = `deliveries` quando o restaurante fornece refrigerante, senão 0; `price(meal)` = Σ quantidade × preço + `deliveries` × taxa + `sodas` × refrigerante. Em `NO_SERVICE` os três são zero. Preços, taxa e refrigerante são lidos na hora, como no custo por dia.
- `Meal.isInPeriod(adjustment)` usa as datas atuais da obra. `getNoServiceDays()` e `getExtraDays()` contam os ajustes de cada tipo no período; `getExtraDaysPrice()` soma os `EXTRA_DAY` no período; `getServiceDays()` = `getWorkingDays() − getNoServiceDays()` (`null` sem término); `getTotalPrice()` = `getDailyPrice() × getServiceDays() + getExtraDaysPrice()` (`null` sem término, como hoje).
- **Alternativa:** ajustes no restaurante. Descartada pelo front: um restaurante pode ter refeições separadas com fornecimentos diferentes no mesmo dia.

### D2. API
```
GET    /api/meals/{id}/adjustments            -> MealAdjustment[] (por data)
POST   /api/meals/{id}/adjustments            { date, type, lunchQuantity, dinnerQuantity, note } -> 201
DELETE /api/meals/{id}/adjustments/{adjId}    -> 204

MealAdjustment { id, date, weekday, type, lunchQuantity, dinnerQuantity, note,
                 deliveries, sodas, price, inPeriod }
MealSummary + { serviceDays, noServiceDays, extraDays, extraDaysPrice }   (totalPrice com ajustes)
Meal (GET /api/meals, /{id}, /by-project/{projectId}, POST/PUT) + { adjustments }
```
- `weekday` = `DayOfWeek` em inglês maiúsculo (`MONDAY` … `SUNDAY`), no padrão dos demais enums da API (`mealType`, `period`). O front traduz para exibição.
- O ajuste não expõe `createdAt`/`updatedAt` (contrato do front); a auditoria fica gravada.
- `MealUseCase.addAdjustment`: carrega a refeição (404 `meal.not_found`), monta o ajuste, chama `meal.addAdjustment`, marca a criação do ajuste e a alteração da refeição (`touch`) e grava com `MealRepository.update`. `removeAdjustment`: 404 `meal.adjustment.not_found` quando o ajuste não é da refeição.
- Segurança: as rotas ficam sob `/api/meals/**`; a regra atual (`GET` para ADMIN e OPERADOR, demais métodos só ADMIN) já cobre.
- `MealMapper.toDomain(existing, dto, restaurant)` leva os ajustes de `existing` para a refeição alterada, para o `PUT` não apagá-los.

### D3. Persistência
- `MealAdjustmentEntity` (`meal_adjustment`), `@ManyToOne` para `MealEntity`. `MealEntity` ganha `@OneToMany(mappedBy = "meal", cascade = ALL, orphanRemoval = true)` com `@BatchSize(size = 100)`, lido dentro das transações dos adaptadores: listagens carregam os ajustes de várias refeições por consulta, sem N+1 por refeição.
- `MealEntityMapper.apply` sincroniza a coleção por `id`: inclui os novos, remove os ausentes e atualiza os existentes.
- Migração `V7__ajustes_por_data_da_refeicao.sql`: `meal_adjustment (id uuid pk, meal_id uuid not null, date date not null, type varchar(20) not null, lunch_quantity integer, dinner_quantity integer, note varchar(100), created_at/updated_at timestamp(6) with time zone not null)`, `fk_meal_adjustment_meal ... on delete cascade`, `uk_meal_adjustment_meal_date unique (meal_id, date)`. SQL comum a PostgreSQL e H2.
- `IntegrityViolationTranslator` mapeia `uk_meal_adjustment_meal_date` para `meal.adjustment.duplicate.date` (corrida entre verificação e gravação).

### D4. Substitui a fórmula de `totalPrice`
`openspec/specs/` está vazio; a nova fórmula entra como requisito ADDED em `meal-date-adjustments`. Ao arquivar, esta change vem depois de `refeicoes-por-obra-e-email-opcional`, `restaurante-entrega-refrigerante-periodos` e `refrigerante-no-restaurante`, e "Estimated total with adjustments" prevalece sobre a fórmula de "Working days and estimated total" e "Daily price breakdown".

## Risks / Trade-offs

- [Ajuste órfão quando o término da obra muda] → guardado, `inPeriod` falso, fora do total. O administrador remove se quiser.
- [Tipo da refeição muda depois do dia extra] → as quantidades gravadas ficam; só as refeições servidas pelo tipo atual entram em `deliveries` e `price`. Documentado no README.
- [`totalPrice` muda para refeições com ajustes] → só com ajustes; o total da obra acompanha porque é calculado na leitura.
- [Leitura de ajustes em toda carga de refeição] → `@BatchSize` limita a uma consulta por lote de refeições.
- [`V7` testada só em H2 nos testes automatizados] → `create table` com tipos e restrições comuns; aplicar numa cópia da base PostgreSQL antes do deploy.

## Migration Plan

1. Deploy do backend: Flyway aplica a `V7` no boot. Nenhum dado existente muda.
2. Deploy do front com a seção de ajustes.
3. Rollback: reverter o código; a tabela `meal_adjustment` pode ficar sem uso.
