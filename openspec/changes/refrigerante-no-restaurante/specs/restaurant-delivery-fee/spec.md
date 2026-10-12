## ADDED Requirements

### Requirement: Optional soda price

A requisição de criação e alteração de restaurante SHALL aceitar `sodaPrice`, opcional: preço unitário do refrigerante, fornecido 1 por entrega em todas as refeições do restaurante. Quando informado, SHALL ser maior que zero; zero ou negativo SHALL responder 400 com `errorCode` `restaurant.invalid.soda_price`, sem gravar. `null` SHALL significar que o restaurante não fornece refrigerante.

#### Scenario: Restaurant with soda

- **WHEN** um restaurante é criado com `sodaPrice` 5.00
- **THEN** a resposta é 201 com `sodaPrice` 5.00 e a leitura seguinte também

#### Scenario: Restaurant without soda

- **WHEN** um restaurante é criado sem `sodaPrice`
- **THEN** a resposta traz `sodaPrice` `null`

#### Scenario: Soda price not greater than zero

- **WHEN** um restaurante é criado ou alterado com `sodaPrice` 0 ou -1
- **THEN** a resposta é 400 com `errorCode` `restaurant.invalid.soda_price` e nada é gravado

### Requirement: Soda price change reflects on meals

Os custos das refeições SHALL usar o preço de refrigerante atual do restaurante em cada leitura.

#### Scenario: Soda price changed

- **WHEN** o refrigerante de um restaurante muda de 5.00 para 6.00 e as refeições dele são lidas
- **THEN** cada refeição traz `sodaPrice` 6.00 e `sodaDailyPrice` = `deliveriesPerDay` × 6.00

#### Scenario: Soda removed

- **WHEN** o restaurante é alterado com `sodaPrice` `null`
- **THEN** as refeições dele trazem `sodaPrice` `null`, `sodaDailyPrice` 0 e `sodas` 0 em cada período

## MODIFIED Requirements

### Requirement: Restaurant summary carries the fee

O resumo do restaurante SHALL trazer `deliveryFee` e `sodaPrice` onde quer que apareça: dentro da refeição (`restaurant`) e na lista de restaurantes da obra.

#### Scenario: Meal shows restaurant fee

- **WHEN** uma refeição de um restaurante com `deliveryFee` 8.00 é lida
- **THEN** `restaurant.deliveryFee` é 8.00

#### Scenario: Meal shows restaurant soda price

- **WHEN** uma refeição de um restaurante com `sodaPrice` 5.00 é lida
- **THEN** `restaurant.sodaPrice` é 5.00
