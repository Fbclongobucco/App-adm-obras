## ADDED Requirements

### Requirement: Semanas de fechamento
A API SHALL listar as semanas de segunda a domingo com algum dia no período da obra, em ordem cronológica, cortando início e fim no período e marcando como parciais as semanas cortadas. Sem término, a lista MUST ir até a semana atual.

#### Scenario: Início e término no meio da semana
- **WHEN** a obra vai de quarta 2026-10-07 a sexta 2026-10-16
- **THEN** as semanas são 2026-10-07 a 2026-10-11 (parcial) e 2026-10-12 a 2026-10-16 (parcial)

#### Scenario: Semana padrão depois do término
- **WHEN** a obra terminou em 2026-10-16, hoje é 2026-11-10 e não há `week`
- **THEN** a semana selecionada começa em 2026-10-12

### Requirement: Fechamento da semana
A API SHALL devolver os 7 dias da semana com situação, almoços, jantares, entregas, refrigerantes e valor, e os totais da semana. A soma das semanas MUST ser igual ao total estimado da refeição.

#### Scenario: Semana com dia extra
- **WHEN** a refeição Almoço e jantar custa R$ 452,00 por dia, a obra vai de 2026-10-05 a 2026-10-16 e 2026-10-10 é dia extra com 6 almoços
- **THEN** a semana de 2026-10-05 tem 6 dias de fornecimento e valor R$ 2.423,00

#### Scenario: Soma igual ao total
- **WHEN** 2026-10-12 está sem fornecimento na mesma refeição
- **THEN** a segunda semana vale R$ 1.808,00 e a soma das semanas é R$ 4.231,00, igual a `totalPrice`

### Requirement: Ajustes no restaurante
Um administrador SHALL incluir feriado ou dia extra em todas as refeições do restaurante numa única operação, que grava tudo ou nada, e remover os ajustes de uma data em todas elas.

#### Scenario: Conflito desfaz tudo
- **WHEN** uma das duas refeições já tem ajuste na data
- **THEN** a API responde 409 `meal.adjustment.duplicate.date` e nenhuma refeição recebe o ajuste

#### Scenario: Dia extra só com quantidade
- **WHEN** o dia extra informa 6 almoços para a refeição Almoço e 0 jantares para a refeição Jantar
- **THEN** só a refeição Almoço recebe o ajuste

#### Scenario: Remoção sem ajuste
- **WHEN** não há ajuste na data informada
- **THEN** a API responde 404

#### Scenario: Operador
- **WHEN** um OPERADOR chama o POST ou o DELETE
- **THEN** a API responde 403
