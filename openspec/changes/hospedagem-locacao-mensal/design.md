## Context

Contrato: `App-adm-obras-front/openspec/changes/hospedagem-locacao-mensal/design.md` (D1 e D2) e spec `locacao-hospedagem`. A situação da locação (cores e dias restantes) é calculada no front com o "hoje" do usuário, como a barra de prazo da obra; a API expõe os períodos.

## Goals / Non-Goals

**Goals:** regras de período no core; migração segura no H2 e no PostgreSQL. **Non-Goals:** pagamentos e alertas fora da tela.

## Decisions

- `AccommodationPeriod` (core, auditado): `id`, `startDate`, `endDate`, `price`, `days()`.
- `Accommodation(id, hostName, hostPhone, address, capacity, isContract, project, startDate, monthlyPrice)` cria o primeiro período. `restorePeriods` reidrata do banco ou da versão anterior; `configureLease(start, monthly)` aplica as regras de edição; `renew(end, price)` e `undoRenewal()`; `nextRenewal()` devolve a sugestão.
- Códigos: `accommodation.invalid.start_date`, `accommodation.invalid.monthly_price`, `accommodation.invalid.renewal_end`, `accommodation.invalid.renewal_price` (400); `accommodation.lease.renewed` e `accommodation.lease.first_period` (409).
- API: `POST /api/accommodations/{id}/renewals` `{ endDate?, price? }` → 201 com a hospedagem; `DELETE /api/accommodations/{id}/renewals/last` → 204.
- V8: fim do mês em SQL com `dateadd`/`interval` comuns ao H2 (MODE=PostgreSQL) e ao PostgreSQL; teste de migração confere a conversão.

## Risks / Trade-offs

- [Remoção de colunas] → backup antes de aplicar em produção; front e backend implantados juntos.
