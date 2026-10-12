## Context

Contrato definido pelo frontend: `App-adm-obras-front/openspec/changes/busca-nas-listagens/design.md` (D1, D2 e D3) e spec `busca-listagens` daquela change. As listagens recebem `page` e `size` (obras também `sort`) e as portas expõem `findAll(PageRequest)`.

## Goals / Non-Goals

**Goals:** regra de normalização única no core; consulta portável entre H2 e PostgreSQL; nenhuma quebra para quem chama sem `q`.

**Non-Goals:** ver proposal.

## Decisions

### D1. `SearchTerm` no core
- `SearchTerm.of(String raw)`: `null` ou só espaços → `SearchTerm.NONE`; acima de 100 caracteres (após remover espaços nas pontas) → `InvalidPageException.invalidQuery` (`page.invalid.query`).
- `text()`: minúsculas, NFD sem diacríticos, `\`, `%` e `_` escapados com `\`. `digits()`: só os dígitos do termo, vazio se não houver.
- As portas ganham `findAll(PageRequest, SearchTerm)`; `findAll(PageRequest)` vira método `default` que delega com `NONE`.

### D2. Consulta com `Specification`
- `SearchSpecification` (infra) monta `translate(lower(campo), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') like '%texto%' escape '\'` para cada campo, unidos por `or`; CPF e CNPJ com `like '%digitos%'` quando `digits()` não é vazio.
- Campos de relação com `left join` a partir da raiz (`client.name`, `project.os`, `address.city`); todas as relações pesquisadas são muitos-para-um, então a contagem não duplica.
- Os repositórios JPA passam a estender `JpaSpecificationExecutor`. A ordenação de obras continua por `SpringPageSupport`.
- **Alternativa:** `@Query` JPQL por recurso. Descartada por repetir a expressão de normalização em 8 consultas e exigir `countQuery` manual.

### D3. API
`GET /api/<recurso>?page=&size=&q=` (obras também `sort`). Use cases ganham `findAll(PageRequest, String query)`.

## Risks / Trade-offs

- [`LIKE '%termo%'` sem índice] → aceitável no volume atual.
- [`TRANSLATE` cobre só os acentos do português] → outros caracteres são comparados literalmente.
