# Tasks

Referência do contrato: `App-adm-obras-front/openspec/changes/hospedagem-locacao-mensal/design.md` (D1 e D2).

## 1. Core

- [x] 1.1 `AccommodationPeriod` e locação em `Accommodation`. Verificar por `AccommodationLeaseTest`: 2026-10-12 → 2026-11-11, 2027-01-31 → 2027-02-28, renovação sugerida e editada, data final inválida, valor inválido, desfazer até o primeiro período, entrada bloqueada após renovar e total.

## 2. Persistência, aplicação e API

- [x] 2.1 V8, entidade do período, mappers, DTOs, use case e endpoints. Verificar por `AccommodationLeaseMigrationTest` e `AccommodationLeaseIntegrationTest` (criar, renovar, desfazer, 409, 400, 403 OPERADOR, total da obra) e pelos testes existentes ajustados.
- [x] 2.2 README, OpenAPI e ERD. Verificar no README e no `.puml`.

## 3. Integração

- [x] 3.1 `./mvnw test` sem falhas e `openspec validate hospedagem-locacao-mensal --strict` válido.
