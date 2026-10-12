## Purpose

Detalha o custo diário da refeição em marmitas, entrega e refrigerante, e projeta quantidades e valores por período de fechamento com os restaurantes.

## ADDED Requirements

### Requirement: Soda per delivery

A requisição de criação e alteração de refeição SHALL aceitar `sodaPrice`, opcional: preço unitário do refrigerante, fornecido 1 por entrega. Quando informado, SHALL ser maior que zero; zero ou negativo SHALL responder 400 com `errorCode` `meal.invalid.soda_price`, sem gravar. `null` SHALL significar refeição sem refrigerante.

#### Scenario: Meal without soda

- **WHEN** uma refeição é criada sem `sodaPrice`
- **THEN** a resposta traz `sodaPrice` `null` e `sodaDailyPrice` 0

#### Scenario: Soda price not greater than zero

- **WHEN** uma refeição é criada com `sodaPrice` 0 ou -1
- **THEN** a resposta é 400 com `errorCode` `meal.invalid.soda_price`

### Requirement: Deliveries per day

Cada refeição servida SHALL contar como uma entrega por dia. A resposta SHALL trazer `deliveriesPerDay`: 1 para `LUNCH` e `DINNER`, 2 para `LUNCH_AND_DINNER`.

#### Scenario: Lunch and dinner

- **WHEN** uma refeição `LUNCH_AND_DINNER` é lida
- **THEN** `deliveriesPerDay` é 2

### Requirement: Daily price breakdown

A resposta da refeição SHALL trazer `mealsDailyPrice` (soma de quantidade × preço de cada refeição servida), `deliveryDailyPrice` (`deliveriesPerDay` × taxa de entrega do restaurante, zero sem taxa), `sodaDailyPrice` (`deliveriesPerDay` × `sodaPrice`, zero sem refrigerante) e `dailyPrice`, a soma das três. `totalPrice` SHALL continuar `dailyPrice × workingDays`.

#### Scenario: Lunch and dinner with fee and soda

- **WHEN** a refeição tem almoço 10 × 25.00 e jantar 8 × 22.00, o restaurante cobra 8.00 por entrega e `sodaPrice` é 5.00
- **THEN** `mealsDailyPrice` é 426.00, `deliveryDailyPrice` 16.00, `sodaDailyPrice` 10.00 e `dailyPrice` 452.00

#### Scenario: Lunch only with fee

- **WHEN** a refeição é `LUNCH` com 10 × 25.00, o restaurante cobra 8.00 e não há refrigerante
- **THEN** `dailyPrice` é 258.00, com `deliveriesPerDay` 1 e `sodaDailyPrice` 0

#### Scenario: No fee and no soda keep current values

- **WHEN** a refeição tem almoço 10 × 25.00 e jantar 8 × 22.00, sem taxa e sem refrigerante
- **THEN** `dailyPrice` é 426.00, igual a `mealsDailyPrice`

#### Scenario: Estimated total includes delivery and soda

- **WHEN** a refeição de `dailyPrice` 452.00 é de uma obra de 2026-10-05 a 2026-10-16
- **THEN** `totalPrice` é 4520.00 e o `totalPrice` da obra inclui esse valor

### Requirement: Billing period projection

A resposta da refeição SHALL trazer `projections`, com os períodos `DAY`, `WEEK`, `FORTNIGHT` e `MONTH`, nessa ordem, de 1, 5, 10 e 22 dias úteis. Cada item SHALL ter `period`, `workingDays`, `lunchMeals`, `dinnerMeals`, `deliveries`, `sodas` e `price`, multiplicando os valores diários pelos dias úteis do período. `sodas` SHALL ser zero sem refrigerante.

#### Scenario: Week

- **WHEN** a refeição tem almoço 10 × 25.00, jantar 8 × 22.00, taxa 8.00 e refrigerante 5.00
- **THEN** `WEEK` traz `workingDays` 5, 50 almoços, 40 jantares, 10 entregas, 10 refrigerantes e `price` 2260.00

#### Scenario: Fortnight and month

- **WHEN** a mesma refeição é lida
- **THEN** `FORTNIGHT` traz `price` 4520.00 e `MONTH` traz 220 almoços, 176 jantares, 44 entregas, 44 refrigerantes e `price` 9944.00

#### Scenario: Meal not served

- **WHEN** a refeição é `LUNCH` sem refrigerante
- **THEN** cada período traz `dinnerMeals` 0 e `sodas` 0

### Requirement: Existing data keeps its values

A migração SHALL adicionar a taxa de entrega do restaurante e o preço do refrigerante da refeição como colunas anuláveis, deixando os registros existentes sem taxa e sem refrigerante.

#### Scenario: Existing meal after migration

- **WHEN** a base tem um restaurante e uma refeição anteriores à migração
- **THEN** após a migração `deliveryFee` e `sodaPrice` são `null` e o `dailyPrice` não muda
