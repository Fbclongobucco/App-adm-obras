## Purpose

Registra exceções de fornecimento por data em cada refeição (dia extra no fim de semana e dia útil sem fornecimento) e as considera no total estimado da refeição e da obra.

## ADDED Requirements

### Requirement: Adjustment endpoints

A API SHALL oferecer `GET /api/meals/{id}/adjustments` (lista ordenada por data), `POST /api/meals/{id}/adjustments` com `{ date, type, lunchQuantity, dinnerQuantity, note }` (201 com o ajuste criado) e `DELETE /api/meals/{id}/adjustments/{adjId}` (204). `type` SHALL ser `EXTRA_DAY` ou `NO_SERVICE`; ausente, 400 `meal.adjustment.invalid.type`. Inclusão e remoção SHALL exigir o papel ADMIN; a leitura SHALL aceitar ADMIN e OPERADOR.

#### Scenario: Create and list

- **WHEN** o administrador inclui um ajuste `NO_SERVICE` em 2026-10-12 numa refeição existente
- **THEN** a resposta é 201 com o ajuste, e `GET /api/meals/{id}/adjustments` o lista

#### Scenario: Remove

- **WHEN** o administrador remove o ajuste de 2026-10-12
- **THEN** a resposta é 204 e o total estimado volta a contar esse dia útil

#### Scenario: Missing type

- **WHEN** um ajuste é incluído sem `type`
- **THEN** a resposta é 400 com `errorCode` `meal.adjustment.invalid.type`

#### Scenario: Unknown meal or adjustment

- **WHEN** a refeição ou o ajuste não existem
- **THEN** a resposta é 404 com `errorCode` `meal.not_found` ou `meal.adjustment.not_found`

#### Scenario: Operator

- **WHEN** um usuário OPERADOR tenta incluir ou remover um ajuste
- **THEN** a resposta é 403 e nada muda; a listagem responde 200

### Requirement: Adjustment response

Cada ajuste SHALL ser respondido como `{ id, date, weekday, type, lunchQuantity, dinnerQuantity, note, deliveries, sodas, price, inPeriod }`. `weekday` SHALL ser o dia da semana em inglês maiúsculo (`MONDAY` a `SUNDAY`). Em `NO_SERVICE`, `deliveries`, `sodas` e `price` SHALL ser zero. `inPeriod` SHALL indicar se a data está no período atual da obra.

#### Scenario: Extra day response

- **WHEN** um dia extra em 2026-10-10 é lido
- **THEN** `weekday` é `SATURDAY` e `inPeriod` é `true`

### Requirement: Adjustment date inside the project period

A data do ajuste SHALL estar dentro do período da obra: igual ou posterior ao início e, quando há término, igual ou anterior a ele. Fora disso, ou sem data, SHALL responder 400 com `errorCode` `meal.adjustment.invalid.date`, sem gravar. Esta regra SHALL ser verificada antes das demais.

#### Scenario: After the end date

- **WHEN** a obra vai de 2026-10-05 a 2026-10-16 e o ajuste é em 2026-10-20
- **THEN** a resposta é 400 com `errorCode` `meal.adjustment.invalid.date`

#### Scenario: Project without end date

- **WHEN** a obra começa em 2026-10-05, não tem término e o ajuste é em 2026-12-05
- **THEN** o ajuste é aceito

### Requirement: One adjustment per date

Cada data SHALL aparecer no máximo uma vez nos ajustes de uma refeição, inclusive entre ajustes fora do período. Repetição SHALL responder 409 com `errorCode` `meal.adjustment.duplicate.date`, sem gravar.

#### Scenario: Same date twice

- **WHEN** a refeição já tem ajuste em 2026-10-12 e outro é incluído na mesma data
- **THEN** a resposta é 409 com `errorCode` `meal.adjustment.duplicate.date`

### Requirement: Extra day on weekends

Um ajuste `EXTRA_DAY` SHALL ser aceito só em sábado ou domingo; em outro dia SHALL responder 400 com `errorCode` `meal.adjustment.invalid.extra_day`.

#### Scenario: Extra day on a weekday

- **WHEN** um `EXTRA_DAY` é incluído na quarta-feira 2026-10-07
- **THEN** a resposta é 400 com `errorCode` `meal.adjustment.invalid.extra_day`

### Requirement: No service on weekdays

Um ajuste `NO_SERVICE` SHALL ser aceito só de segunda a sexta; em sábado ou domingo SHALL responder 400 com `errorCode` `meal.adjustment.invalid.no_service`.

#### Scenario: No service on Saturday

- **WHEN** um `NO_SERVICE` é incluído no sábado 2026-10-10
- **THEN** a resposta é 400 com `errorCode` `meal.adjustment.invalid.no_service`

### Requirement: Adjustment quantities

No `EXTRA_DAY`, a quantidade de cada refeição servida pelo tipo SHALL ser inteira de 0 a 10000, com ao menos uma maior que zero; a da refeição não servida SHALL ser ignorada. No `NO_SERVICE`, as quantidades SHALL ser nulas. Violação SHALL responder 400 com `errorCode` `meal.adjustment.invalid.quantity`.

#### Scenario: All quantities zero

- **WHEN** um `EXTRA_DAY` de uma refeição `LUNCH_AND_DINNER` tem almoço 0 e jantar 0
- **THEN** a resposta é 400 com `errorCode` `meal.adjustment.invalid.quantity`

#### Scenario: Missing served quantity

- **WHEN** um `EXTRA_DAY` de uma refeição `LUNCH_AND_DINNER` informa só o almoço
- **THEN** a resposta é 400 com `errorCode` `meal.adjustment.invalid.quantity`

#### Scenario: Quantity on no service

- **WHEN** um `NO_SERVICE` informa `lunchQuantity` 5
- **THEN** a resposta é 400 com `errorCode` `meal.adjustment.invalid.quantity`

### Requirement: Adjustment note

A observação SHALL ser opcional, com até 100 caracteres após remover espaços nas pontas; vazia SHALL virar `null`. Mais longa SHALL responder 400 com `errorCode` `meal.adjustment.invalid.note`.

#### Scenario: Note too long

- **WHEN** um ajuste é incluído com observação de 101 caracteres
- **THEN** a resposta é 400 com `errorCode` `meal.adjustment.invalid.note`

### Requirement: Extra day price

O valor do dia extra SHALL somar: quantidade × preço de cada refeição servida com quantidade maior que zero; uma entrega por refeição com quantidade maior que zero, à taxa do restaurante; e um refrigerante por entrega, ao preço do restaurante, quando ele fornece. `deliveries` e `sodas` SHALL trazer essas contagens.

#### Scenario: Saturday with lunch only

- **WHEN** a refeição `LUNCH_AND_DINNER` tem almoço a 25.00, o restaurante cobra 8.00 de entrega e fornece refrigerante a 5.00, e o dia extra 2026-10-10 tem 6 almoços e 0 jantares
- **THEN** o ajuste traz `deliveries` 1, `sodas` 1 e `price` 163.00

### Requirement: Estimated total with adjustments

A refeição SHALL trazer `noServiceDays` e `extraDays` (ajustes de cada tipo no período atual da obra), `extraDaysPrice` (soma dos dias extras no período) e `serviceDays` = `workingDays − noServiceDays`. `totalPrice` SHALL ser `dailyPrice × serviceDays + extraDaysPrice`; sem término da obra, `serviceDays` e `totalPrice` SHALL ser `null`. O total da obra SHALL somar esse `totalPrice`.

#### Scenario: Two weeks with adjustments

- **WHEN** a obra vai de 2026-10-05 a 2026-10-16, o `dailyPrice` é 452.00, 2026-10-12 é `NO_SERVICE` e 2026-10-10 é `EXTRA_DAY` de 163.00
- **THEN** `workingDays` é 10, `noServiceDays` 1, `serviceDays` 9, `extraDays` 1, `extraDaysPrice` 163.00 e `totalPrice` 4231.00, e o `totalPrice` da obra inclui 4231.00

#### Scenario: Without adjustments

- **WHEN** a refeição não tem ajustes
- **THEN** `serviceDays` é igual a `workingDays`, `noServiceDays` e `extraDays` são 0 e `totalPrice` não muda

### Requirement: Adjustments outside the current period

Quando o período da obra muda, os ajustes fora do novo período SHALL ser mantidos, com `inPeriod` `false`, e MUST NOT entrar em `noServiceDays`, `extraDays`, `extraDaysPrice` nem `totalPrice`.

#### Scenario: End date moved earlier

- **WHEN** a obra tem ajustes em 2026-10-10 e 2026-10-12 e o término muda de 2026-10-16 para 2026-10-09
- **THEN** os dois ajustes continuam na lista com `inPeriod` `false` e `totalPrice` é `dailyPrice × 5`

### Requirement: Meal detail lists adjustments

A resposta de detalhe da refeição SHALL trazer `adjustments`, ordenados por data. Excluir a refeição SHALL excluir os ajustes dela.

#### Scenario: Meal detail

- **WHEN** `GET /api/meals/{id}` é lido para uma refeição com ajustes em 2026-10-12 e 2026-10-10
- **THEN** `adjustments` traz 2026-10-10 e depois 2026-10-12

#### Scenario: Meal deleted

- **WHEN** a refeição com ajustes é excluída
- **THEN** a resposta é 204 e os ajustes deixam de existir
