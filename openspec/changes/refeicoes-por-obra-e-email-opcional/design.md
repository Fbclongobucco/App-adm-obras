# Design

## Context

- Contrato definido pelo frontend em `App-adm-obras-front/openspec/changes/melhorias-design-refeicoes-cadastros/design.md` (D1, D2, D7). Esta change o implementa literalmente. Motivação: ver `proposal.md`.
- `Meal` hoje: `restaurant`, `price`, `quantity`, `date`, `mealType` (`LUNCH|DINNER`), `totalPrice = price × quantity`, gravado em `meal.total_price`. A obra da refeição é a do restaurante (`meal.project_id` é cópia, para `uk_meal_project_type`).
- `Project` hoje tem `lunch` e `dinner` e um campo `totalPrice` recalculado em `refreshTotalPrice()`, chamado por `Accommodation`. `ProjectRepositoryAdapter` lê almoço e jantar com `findFirstByProjectIdAndMealType`. `ProjectUseCase` valida `lunchId`/`dinnerId`.
- Bancos: PostgreSQL em `prod`/compose; H2 2.x em memória com `MODE=PostgreSQL` nos perfis `local`, `test` e nos testes. Não há Testcontainers. As migrações precisam rodar nos dois.
- `Client.validateEmail` e `Employee.validateEmail` rejeitam `null`. `ClientUseCase`/`EmployeeUseCase` checam duplicidade com `findByEmail`; o Spring Data traduz `findFirstByEmail(null)` em `IS NULL`, o que acusaria conflito entre registros sem e-mail.
- Não há seed de dados no backend: foi removido em `auditoria-restaurante-e-conflitos` (tarefa 6.1). O seed fica em `App-adm-obras-front/scripts/seed.mjs`.

## Goals / Non-Goals

**Goals:**
- Regra de cobertura, validação por refeição servida, custo por dia, dias úteis e total no domínio (`core/domain`). DTO, mapper, entidade e migração só acompanham.
- Total estimado sempre consistente com as datas atuais da obra, sem valor congelado no banco.
- Unicidade de cobertura garantida também no banco, em PostgreSQL e H2.

**Non-Goals:**
- Feriados no cálculo de dias úteis.
- Recalcular `project.total_price` persistido a cada mutação de refeição (ver `endurecer-dominio-e-persistencia`).
- Seed de dados no backend.

## Decisions

### D1. `MealType` sabe o que serve
`MealType` ganha `LUNCH_AND_DINNER` e os métodos `servesLunch()` e `servesDinner()`. `Meal.overlaps(other)` é verdadeiro quando os ids diferem e as coberturas se cruzam. O caso de uso usa `overlaps` contra as refeições da obra e lança `ConflictException.duplicate("meal", "type", ...)` (`meal.duplicate.type`, 409). `Project.addMeal` aplica a mesma regra como invariante (`project.duplicated_meal_type`) para o agregado carregado.
- **Alternativa:** dois registros por "almoço e jantar". Descartada pelo front (D2): vira um par a sincronizar.

### D2. Campos por refeição servida, definidos juntos
`Meal.configure(mealType, lunchQuantity, lunchPrice, dinnerQuantity, dinnerPrice)` valida o tipo primeiro e, para cada refeição servida, quantidade (1..10.000) e preço (> 0). Campos de refeição não servida são descartados (`null`). Um setter por campo foi descartado: trocar o tipo exigiria revalidar os outros campos em ordem.

### D3. Custos calculados na leitura, a partir da obra atual
- `Project.getWorkingDays()`: dias de segunda a sexta de `startDate` a `endDate`, inclusive; `null` sem `endDate`. Cálculo em O(1): semanas inteiras × 5 mais o resto, sem iterar dia a dia.
- `Meal.getDailyPrice()`: soma de quantidade × preço das refeições servidas.
- `Meal.getWorkingDays()` delega para `project.getWorkingDays()`; `Meal.getTotalPrice()` = `dailyPrice × workingDays`, ou `null`.
- `Project.getTotalPrice()` passa a ser calculado em cada chamada (refeições com total nulo contam zero, mais hospedagens). O campo e `refreshTotalPrice()` deixam de existir; `Accommodation` não precisa mais avisar a obra.
- `Project.addMeal(meal)` liga `meal.project` à própria obra, para que os custos usem as datas da instância que está sendo respondida (inclusive no PUT da obra, que reconstrói o agregado).
- `meal.total_price` é removida: um valor gravado ficaria errado ao mudar o término da obra.
- **Alternativa:** gravar `working_days`/`total_price` e recalcular ao alterar a obra. Descartada: exige propagar alterações da obra para as refeições.

### D4. Unicidade no banco com colunas de cobertura
`meal` ganha `lunch_slot` e `dinner_slot` (`boolean`, `true` quando a refeição serve, `null` quando não) e as restrições `uk_meal_project_lunch unique (project_id, lunch_slot)` e `uk_meal_project_dinner unique (project_id, dinner_slot)`. PostgreSQL e H2 tratam `NULL` como distinto em `UNIQUE`, então só as coberturas ocupadas conflitam. As colunas são derivadas do tipo pelo mapper de entidade, nunca lidas de volta para o domínio. `IntegrityViolationTranslator` mapeia as duas para `meal.duplicate.type`.
- **Alternativa:** índice único parcial (`where meal_type in (...)`). Descartada: H2 não suporta índice parcial.
- **Alternativa:** `CHECK (lunch_slot)` para impedir `false`. Omitida pelo mesmo cuidado com `CHECK` no H2 descrito no `V1`; o mapper só grava `true` ou `null`.

### D5. Obra expõe `meals`; vínculo só pelo restaurante
`Project` troca `lunch`/`dinner` por uma coleção de refeições ordenada por tipo (`LUNCH`, `DINNER`, `LUNCH_AND_DINNER`) e data de criação. `ProjectRepositoryAdapter` carrega `findByProjectId`. `ProjectRequestDTO` perde `lunchId`/`dinnerId`; `ProjectUseCase.update` copia as refeições da obra existente para a obra alterada, como já faz com restaurantes. `MealSummaryDTO` (sem `project`) evita ciclo na resposta da obra.

### D6. E-mail opcional no domínio
`Client.validateEmail` e `Employee.validateEmail`: `null` ou em branco retorna `null`; caso contrário, mesmo padrão e mesmos códigos. Os casos de uso só checam duplicidade quando o e-mail não é nulo. `V4` remove `not null` das colunas; `uk_client_email` e `uk_employee_email` seguem.

### D7. Migração `V4__refeicoes_por_obra_e_email_opcional.sql`
1. Adiciona `lunch_quantity`, `lunch_price`, `dinner_quantity`, `dinner_price`, `lunch_slot`, `dinner_slot` (anuláveis).
2. `LUNCH` → `lunch_quantity = quantity`, `lunch_price = price`, `lunch_slot = true`; `DINNER` → equivalente de jantar.
3. Remove `uk_meal_project_type` e cria `uk_meal_project_lunch`/`uk_meal_project_dinner`. Os dados atuais não violam: havia no máximo um `LUNCH` e um `DINNER` por obra.
4. Remove `date`, `quantity`, `price`, `total_price`.
5. `client.email` e `employee.email` anuláveis.
Só SQL comum a PostgreSQL e H2 (`alter table ... add column`, `update`, `drop constraint`, `drop column`, `alter column ... drop not null`).

## Risks / Trade-offs

- [Contrato quebra entre front e backend] → publicação conjunta, conforme o design do front (Risks). Os códigos `meal.invalid.price`, `meal.invalid.quantity`, `meal.invalid.date` e `meal.invalid.date.future` deixam de existir; o `errors.ts` do front deve ser ajustado.
- [Total estimado superestima por ignorar feriados] → documentado no README e no rótulo do front.
- [`project.total_price` persistido fica defasado em relação ao calculado] → já ocorria antes desta change; a API devolve o valor calculado. Tratado em `endurecer-dominio-e-persistencia`.
- [V4 testada só em H2 nos testes automatizados] → SQL restrito ao comum. Verificado manualmente em PostgreSQL 16 (container): V1–V3 com refeições `LUNCH`/`DINNER`, aplicação da `V4`, conversão conferida, `uk_meal_project_lunch` rejeitando segunda cobertura e dois clientes sem e-mail aceitos; boot do perfil `prod` aplicando V1–V4 do zero. Repetir na base real antes do deploy (Migration Plan).
- [`meal-assignment-uniqueness` (deploy-viavel) e `project-cost-totalization` (endurecer) não arquivadas] → esta change substitui a regra por tipo pela regra de cobertura e o total por refeição pelo total estimado. Ao arquivar, `project-meal-plan` prevalece.

## Migration Plan

1. Backup da base de produção.
2. Deploy do backend: Flyway aplica `V4` no boot.
3. Conferir: `select meal_type, lunch_quantity, lunch_price, dinner_quantity, dinner_price from meal` sem refeição com campos do tipo nulos.
4. Deploy do front com o contrato novo.
5. Rollback: reverter front e backend juntos e restaurar o backup. `V4` não tem reversão automática (descarta `date`).
