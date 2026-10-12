## ADDED Requirements

### Requirement: Locação mensal
A hospedagem SHALL ter data de entrada e valor por mês maior que zero. O primeiro período MUST ir da entrada até a véspera do mesmo dia no mês seguinte, ou até o último dia do mês seguinte quando o dia não existe nele. O total MUST ser a soma dos períodos.

#### Scenario: Primeiro período
- **WHEN** a entrada é 2026-10-12 com 3000.00 por mês
- **THEN** o período é de 2026-10-12 a 2026-11-11 e o total é 3000.00

#### Scenario: Fim de mês
- **WHEN** a entrada é 2027-01-31
- **THEN** o período termina em 2027-02-28

### Requirement: Renovação
A API SHALL incluir um período a partir do dia seguinte ao término atual, com data final e valor informados ou sugeridos (+1 mês e valor mensal), rejeitando data final anterior ao início e valor menor ou igual a zero com 400.

#### Scenario: Renovação sugerida
- **WHEN** o término é 2026-11-11 e a renovação vem sem corpo
- **THEN** entra o período de 2026-11-12 a 2026-12-11 pelo valor mensal

### Requirement: Desfazer e edição
A API SHALL remover a última renovação e MUST recusar com 409 a remoção do primeiro período e a mudança da entrada quando houver renovações.

#### Scenario: Entrada após renovar
- **WHEN** há renovações e o PUT muda a entrada
- **THEN** responde 409 `accommodation.lease.renewed`
