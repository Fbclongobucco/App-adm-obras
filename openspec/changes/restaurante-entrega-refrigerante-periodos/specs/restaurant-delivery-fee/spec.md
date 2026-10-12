## Purpose

Registra a taxa de entrega opcional cobrada por restaurante e expõe, na resposta do restaurante, as refeições que ele serve com seus custos, para o fechamento por restaurante.

## ADDED Requirements

### Requirement: Optional delivery fee

A requisição de criação e alteração de restaurante SHALL aceitar `deliveryFee`, opcional, em reais, cobrada por entrega. Quando informada, SHALL ser maior que zero; zero ou negativa SHALL responder 400 com `errorCode` `restaurant.invalid.delivery_fee`, sem gravar. `null` SHALL significar restaurante sem taxa.

#### Scenario: Restaurant without fee

- **WHEN** um restaurante é criado sem `deliveryFee`
- **THEN** a resposta é 201 com `deliveryFee` `null`

#### Scenario: Restaurant with fee

- **WHEN** um restaurante é criado com `deliveryFee` 8.00
- **THEN** a resposta traz `deliveryFee` 8.00 e a leitura seguinte também

#### Scenario: Fee not greater than zero

- **WHEN** um restaurante é criado ou alterado com `deliveryFee` 0 ou -1
- **THEN** a resposta é 400 com `errorCode` `restaurant.invalid.delivery_fee` e nada é gravado

### Requirement: Restaurant summary carries the fee

O resumo do restaurante SHALL trazer `deliveryFee` onde quer que apareça: dentro da refeição (`restaurant`) e na lista de restaurantes da obra.

#### Scenario: Meal shows restaurant fee

- **WHEN** uma refeição de um restaurante com `deliveryFee` 8.00 é lida
- **THEN** `restaurant.deliveryFee` é 8.00

### Requirement: Restaurant response lists its meals

A resposta do restaurante (criação, alteração, detalhe, listagem paginada e listagem por obra) SHALL trazer `meals`, a lista resumida das refeições servidas por ele, com os mesmos campos e custos da refeição resumida. Restaurante sem refeição SHALL trazer `meals` vazio.

#### Scenario: Restaurant with meal

- **WHEN** um restaurante com uma refeição `LUNCH_AND_DINNER` é lido por `GET /api/restaurants/{id}`
- **THEN** `meals` tem um item com `mealType` `LUNCH_AND_DINNER`, `dailyPrice` e `projections`

#### Scenario: Restaurant list

- **WHEN** `GET /api/restaurants` lista restaurantes com e sem refeições
- **THEN** cada item traz `meals` com as refeições daquele restaurante, ou lista vazia

### Requirement: Fee change reflects on meals

Os custos das refeições SHALL usar a taxa atual do restaurante em cada leitura.

#### Scenario: Fee changed

- **WHEN** a taxa de um restaurante muda de `null` para 8.00 e a refeição `LUNCH` dele é lida
- **THEN** `deliveryDailyPrice` é 8.00 e `dailyPrice` aumenta 8.00
