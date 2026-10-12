## Why

A hospedagem é cadastrada com diárias e valor total, mas a locação real é mensal e renovada conforme a necessidade. O frontend (change `hospedagem-locacao-mensal`, design D1 e D2) definiu o contrato e depende desta change.

## What Changes

- **BREAKING:** `days` e `totalPrice` saem do pedido; `totalPrice` passa a ser a soma dos períodos.
- Hospedagem com `startDate` e `monthlyPrice`; o primeiro período vai da entrada até a véspera do mesmo dia no mês seguinte (último dia do mês quando o dia não existe).
- Renovação: novo período a partir do dia seguinte ao término, com data final e valor opcionais (sugestão: +1 mês e valor mensal). Desfazer a última renovação; o primeiro período não sai.
- Entrada só muda sem renovações; com renovações, mudar a entrada responde 409.
- Migração V8: tabela `accommodation_period`, conversão dos dados atuais (de teste) e remoção de `days` e `total_price`.

## Capabilities

### New Capabilities

- `accommodation-lease`: locação mensal da hospedagem, períodos, renovação e total.

### Modified Capabilities

<!-- Nenhuma. -->

## Impact

- Core: `Accommodation`, `AccommodationPeriod`, exceções. Aplicação: DTOs, mapper, use case. Infra: entidade, mapper, controller, V8. Testes, README e ERD.
