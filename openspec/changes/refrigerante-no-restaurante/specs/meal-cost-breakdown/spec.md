## ADDED Requirements

### Requirement: Meal soda price comes from the restaurant

A requisição de criação e alteração de refeição MUST NOT ler `sodaPrice`; o campo, se enviado, SHALL ser ignorado sem erro. A resposta da refeição SHALL trazer `sodaPrice` igual ao `sodaPrice` do restaurante, somente leitura.

#### Scenario: Soda price sent on meal

- **WHEN** uma refeição é criada com `sodaPrice` 9.00 num restaurante sem refrigerante
- **THEN** a resposta é 201 com `sodaPrice` `null` e `sodaDailyPrice` 0

#### Scenario: Meal reads restaurant soda

- **WHEN** uma refeição é criada num restaurante com `sodaPrice` 5.00
- **THEN** a resposta traz `sodaPrice` 5.00

### Requirement: Existing soda prices move to the restaurant

A migração SHALL criar o preço do refrigerante no restaurante com o maior preço informado entre as refeições dele, e remover o preço da refeição. Restaurante sem refeição com refrigerante SHALL ficar sem refrigerante.

#### Scenario: Different prices

- **WHEN** um restaurante tem uma refeição com refrigerante 5.00 e outra com 6.00 antes da migração
- **THEN** após a migração o restaurante tem `sodaPrice` 6.00

#### Scenario: Mixed restaurant

- **WHEN** um restaurante tem uma refeição com refrigerante 5.00 e outra sem refrigerante antes da migração
- **THEN** após a migração o restaurante tem `sodaPrice` 5.00 e as duas refeições passam a ter refrigerante

#### Scenario: No soda

- **WHEN** nenhuma refeição do restaurante tinha refrigerante antes da migração
- **THEN** após a migração o restaurante tem `sodaPrice` `null`

## MODIFIED Requirements

### Requirement: Daily price breakdown

A resposta da refeição SHALL trazer `mealsDailyPrice` (soma de quantidade × preço de cada refeição servida), `deliveryDailyPrice` (`deliveriesPerDay` × taxa de entrega do restaurante, zero sem taxa), `sodaDailyPrice` (`deliveriesPerDay` × preço do refrigerante do restaurante, zero quando o restaurante não fornece) e `dailyPrice`, a soma das três. `totalPrice` SHALL continuar `dailyPrice × workingDays`.

#### Scenario: Lunch and dinner with fee and soda

- **WHEN** a refeição tem almoço 10 × 25.00 e jantar 8 × 22.00, e o restaurante cobra 8.00 por entrega e fornece refrigerante a 5.00
- **THEN** `mealsDailyPrice` é 426.00, `deliveryDailyPrice` 16.00, `sodaDailyPrice` 10.00 e `dailyPrice` 452.00

#### Scenario: Lunch only with fee

- **WHEN** a refeição é `LUNCH` com 10 × 25.00, o restaurante cobra 8.00 e não fornece refrigerante
- **THEN** `dailyPrice` é 258.00, com `deliveriesPerDay` 1 e `sodaDailyPrice` 0

#### Scenario: No fee and no soda keep current values

- **WHEN** a refeição tem almoço 10 × 25.00 e jantar 8 × 22.00, e o restaurante não cobra taxa nem fornece refrigerante
- **THEN** `dailyPrice` é 426.00, igual a `mealsDailyPrice`

#### Scenario: Estimated total includes delivery and soda

- **WHEN** a refeição de `dailyPrice` 452.00 é de uma obra de 2026-10-05 a 2026-10-16
- **THEN** `totalPrice` é 4520.00 e o `totalPrice` da obra inclui esse valor

## REMOVED Requirements

### Requirement: Soda per delivery

**Reason**: O preço do refrigerante passa a ser condição do restaurante, cadastrado junto da taxa de entrega, e não de cada refeição.
**Migration**: Informar `sodaPrice` em `POST/PUT /api/restaurants` (requisito "Optional soda price" de `restaurant-delivery-fee`). A migração `V6` copia para cada restaurante o maior preço já informado nas refeições dele.
