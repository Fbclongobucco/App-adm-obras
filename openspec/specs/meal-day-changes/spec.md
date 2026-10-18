# meal-day-changes Specification

## Purpose
Taxa de entrega separada para almoço e jantar, edição dos números de um dia (dia alterado e dia extra) com entregas e refrigerantes, e gravação do dia por data na refeição e no restaurante.

## Requirements

### Requirement: Taxa de entrega por refeição
O restaurante SHALL ter `lunchDeliveryFee` e `dinnerDeliveryFee`, opcionais e maiores que zero. O custo de entrega por dia da refeição MUST ser a taxa do almoço, se serve almoço, mais a taxa do jantar, se serve jantar. A migração MUST copiar a taxa anterior para as duas.

#### Scenario: Taxas diferentes
- **WHEN** o restaurante tem entrega do almoço R$ 8,00, do jantar R$ 10,00 e refrigerante R$ 5,00, e a refeição Almoço e jantar tem 10 × R$ 25,00 e 8 × R$ 22,00
- **THEN** `deliveryDailyPrice` é R$ 18,00 e `dailyPrice` é R$ 454,00

#### Scenario: Taxa inválida
- **WHEN** uma das taxas é zero ou negativa
- **THEN** a API responde 400 `restaurant.invalid.delivery_fee`

### Requirement: Dia alterado
O ajuste `CHANGED` SHALL ser aceito só de segunda a sexta, com as quantidades das refeições servidas de 0 a 10.000 e ao menos uma maior que zero. O valor do dia MUST ser calculado como o do dia extra.

#### Scenario: Uma janta a menos
- **WHEN** a refeição de R$ 454,00 por dia é alterada em 14/10/2026 para 10 almoços e 7 jantares
- **THEN** o dia vale R$ 432,00

#### Scenario: Fim de semana
- **WHEN** o ajuste `CHANGED` é num sábado
- **THEN** a API responde 400 `meal.adjustment.invalid.changed_day`

### Requirement: Entregas e refrigerantes do dia
Em `EXTRA_DAY` e `CHANGED`, `lunchDeliveries` e `dinnerDeliveries` (0 a 100) e `sodas` (0 a 10.000) SHALL ser opcionais. Vazio MUST valer o padrão: 1 entrega por refeição com quantidade maior que zero e 1 refrigerante por entrega quando o restaurante fornece. Entrega maior que zero com quantidade zero MUST ser rejeitada com `meal.adjustment.invalid.delivery_without_meal`; refrigerante maior que zero sem refrigerante no restaurante, com `meal.adjustment.invalid.soda_not_supplied`.

#### Scenario: Duas entregas no almoço
- **WHEN** o dia é alterado para 10 almoços, 9 jantares, 2 entregas do almoço e 1 do jantar, sem refrigerantes
- **THEN** o dia tem 3 entregas, 3 refrigerantes e vale R$ 489,00

#### Scenario: Refrigerantes informados
- **WHEN** o mesmo dia tem 2 refrigerantes
- **THEN** o dia vale R$ 484,00

### Requirement: Gravação do dia por data
`PUT /api/meals/{id}/adjustments/{date}` SHALL criar ou substituir o ajuste da data, mantendo o id. `PUT /api/restaurants/{id}/adjustments/{date}` SHALL gravar o dia em todas as refeições do restaurante de uma vez: em `CHANGED`, refeição zerada fica `NO_SERVICE` e refeição igual ao padrão fica sem ajuste; em `EXTRA_DAY`, só as refeições com quantidade recebem o ajuste. Qualquer erro MUST deixar todas as refeições como estavam.

#### Scenario: Substituir dia extra
- **WHEN** o sábado com 6 almoços (R$ 163,00) é regravado com 6 almoços e 4 jantares
- **THEN** o ajuste mantém o id e vale R$ 266,00

#### Scenario: Jantar suspenso no restaurante
- **WHEN** o restaurante com Almoço e Jantar grava `CHANGED` em 14/10/2026 com 10 almoços e 0 jantares
- **THEN** a refeição Jantar fica `NO_SERVICE`, a Almoço fica sem ajuste e o dia do restaurante tem 10 almoços e 1 entrega

### Requirement: Fechamento e total com o dia alterado
O fechamento SHALL ter a situação `CHANGED` e, por dia, `lunchDeliveries`, `dinnerDeliveries` e `note`; no restaurante, `meals` com os números de cada refeição. O total estimado MUST ser a soma do valor de todos os dias do período.

#### Scenario: Total com dia alterado
- **WHEN** a obra vai de 05/10/2026 a 16/10/2026, a refeição custa R$ 454,00 por dia e 14/10/2026 vale R$ 489,00
- **THEN** o total estimado é R$ 4.575,00 e a semana de 12/10 vale R$ 2.305,00
