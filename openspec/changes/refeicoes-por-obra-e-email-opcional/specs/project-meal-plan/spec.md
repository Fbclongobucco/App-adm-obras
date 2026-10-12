## Purpose

Registra a alimentação contratada para cada obra junto a um restaurante (refeições servidas, quantidade e preço por dia) e informa o custo por dia, os dias úteis e o total estimado até o término da obra.

## ADDED Requirements

### Requirement: Meal is a project configuration without date

Uma refeição SHALL representar a alimentação contratada de uma obra junto a um restaurante, sem data. A obra da refeição SHALL ser a obra do restaurante. A requisição de criação e alteração SHALL ter `restaurantId`, `mealType`, `isBilled`, `lunchQuantity`, `lunchPrice`, `dinnerQuantity` e `dinnerPrice`, e SHALL NOT ter `date`, `quantity` nem `price`.

#### Scenario: Meal inherits the restaurant project

- **WHEN** uma refeição é criada com um restaurante da obra OS-123
- **THEN** a resposta traz a obra OS-123 em `project`

#### Scenario: Missing restaurant is rejected

- **WHEN** uma refeição é criada sem `restaurantId`
- **THEN** a resposta é 400 com `errorCode` `meal.invalid.restaurant`

### Requirement: Meal types

`mealType` SHALL aceitar `LUNCH`, `DINNER` e `LUNCH_AND_DINNER`. Tipo ausente SHALL responder 400 `meal.invalid.type`.

#### Scenario: Missing type

- **WHEN** uma refeição é criada sem `mealType`
- **THEN** a resposta é 400 com `errorCode` `meal.invalid.type`

### Requirement: Quantity and price per served meal

Para cada refeição servida pelo tipo, a quantidade por dia SHALL ser inteira de 1 a 10.000 e o preço unitário SHALL ser maior que zero. `LUNCH` e `LUNCH_AND_DINNER` servem almoço; `DINNER` e `LUNCH_AND_DINNER` servem jantar. Os campos de refeição não servida SHALL ser ignorados e devolvidos `null`. Os códigos de erro SHALL ser `meal.invalid.lunch_quantity`, `meal.invalid.lunch_price`, `meal.invalid.dinner_quantity` e `meal.invalid.dinner_price`.

#### Scenario: Lunch only

- **WHEN** uma refeição `LUNCH` é criada com `lunchQuantity` 10, `lunchPrice` 25.00 e `dinnerQuantity` 5
- **THEN** a resposta traz almoço 10 × 25.00 e `dinnerQuantity` e `dinnerPrice` `null`

#### Scenario: Lunch and dinner require both

- **WHEN** uma refeição `LUNCH_AND_DINNER` é criada sem `dinnerPrice`
- **THEN** a resposta é 400 com `errorCode` `meal.invalid.dinner_price`

#### Scenario: Invalid quantity

- **WHEN** uma refeição `LUNCH` é criada com `lunchQuantity` 0 ou 10001
- **THEN** a resposta é 400 com `errorCode` `meal.invalid.lunch_quantity`

#### Scenario: Price not greater than zero

- **WHEN** uma refeição `DINNER` é criada com `dinnerPrice` 0
- **THEN** a resposta é 400 com `errorCode` `meal.invalid.dinner_price`

### Requirement: Daily price

`dailyPrice` SHALL ser a soma de quantidade × preço de cada refeição servida.

#### Scenario: Lunch and dinner daily price

- **WHEN** a refeição tem almoço 10 × 25.00 e jantar 8 × 22.00
- **THEN** `dailyPrice` é 426.00

### Requirement: Working days and estimated total

`workingDays` SHALL ser o número de dias de segunda a sexta entre a data de início e a data de término da obra, ambas incluídas, sem descontar feriados. `totalPrice` SHALL ser `dailyPrice × workingDays`. Sem data de término, `workingDays` e `totalPrice` SHALL ser `null`. Os valores SHALL refletir as datas atuais da obra em cada leitura.

#### Scenario: Two weeks

- **WHEN** a obra vai de 2026-10-05 a 2026-10-16 e o `dailyPrice` é 426.00
- **THEN** `workingDays` é 10 e `totalPrice` é 4260.00

#### Scenario: Project without end date

- **WHEN** a obra da refeição não tem data de término
- **THEN** `dailyPrice` é informado e `workingDays` e `totalPrice` são `null`

#### Scenario: Project end date changes

- **WHEN** a data de término da obra é alterada de 2026-10-16 para 2026-10-09
- **THEN** a leitura seguinte da refeição traz `workingDays` 5 e `totalPrice` recalculado

### Requirement: One lunch coverage and one dinner coverage per project

Cada obra SHALL ter no máximo uma refeição que sirva almoço e no máximo uma que sirva jantar. `LUNCH_AND_DINNER` ocupa as duas coberturas. Violação na criação ou alteração SHALL responder 409 com `errorCode` `meal.duplicate.type`, sem gravar. O banco SHALL rejeitar a sobreposição mesmo fora da aplicação, com o mesmo código.

#### Scenario: Lunch after lunch and dinner

- **WHEN** a obra tem uma refeição `LUNCH_AND_DINNER` e outra `LUNCH` é criada
- **THEN** a resposta é 409 com `errorCode` `meal.duplicate.type`

#### Scenario: Separate lunch and dinner

- **WHEN** a obra tem uma refeição `LUNCH` e uma `DINNER` é criada
- **THEN** a refeição é gravada

#### Scenario: Editing a meal keeps its own coverage

- **WHEN** a única refeição `LUNCH` da obra é alterada para `LUNCH_AND_DINNER` e a obra não tem jantar
- **THEN** a alteração é aceita

### Requirement: Project exposes its meals

A resposta de obra SHALL trazer `meals`, a lista resumida das refeições da obra, e SHALL NOT trazer `lunch` nem `dinner`. A requisição de obra SHALL NOT ter `lunchId` nem `dinnerId`. Alterar a obra SHALL manter as refeições vinculadas pelo restaurante.

#### Scenario: Project read

- **WHEN** a obra com uma refeição `LUNCH_AND_DINNER` é lida
- **THEN** `meals` tem um item com `mealType` `LUNCH_AND_DINNER`, custos calculados e sem `project`

#### Scenario: Project update keeps meals

- **WHEN** a obra é alterada por PUT
- **THEN** a leitura seguinte traz as mesmas refeições em `meals`

### Requirement: Project total includes estimated meal totals

O `totalPrice` da obra SHALL ser a soma do `totalPrice` de cada refeição (`null` conta como zero) com o total das hospedagens vinculadas.

#### Scenario: Project with end date

- **WHEN** a obra de 2026-10-05 a 2026-10-16 tem uma refeição de `dailyPrice` 426.00 e nenhuma hospedagem
- **THEN** o `totalPrice` da obra é 4260.00

#### Scenario: Project without end date

- **WHEN** a obra sem término tem refeições
- **THEN** as refeições não somam no `totalPrice` da obra

### Requirement: Existing meals are migrated

A migração SHALL converter cada refeição `LUNCH` existente em quantidade e preço de almoço e cada `DINNER` em quantidade e preço de jantar, preservando id, restaurante, obra, faturamento e datas de auditoria, e SHALL descartar a data da refeição.

#### Scenario: Lunch row migrated

- **WHEN** a base tem uma refeição `LUNCH` com quantidade 12 e preço 30.00
- **THEN** após a migração ela tem `lunchQuantity` 12, `lunchPrice` 30.00 e campos de jantar `null`
