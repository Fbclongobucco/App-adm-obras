## Why

As listagens do frontend só mostram a página atual, e não há como localizar um registro sem percorrer as páginas. O frontend (change `busca-nas-listagens`, design D1 a D3) já envia o termo em `q` e depende desta change para filtrar.

## What Changes

- As 8 listagens paginadas (`projects`, `clients`, `employees`, `cost-centers`, `restaurants`, `meals`, `accommodations`, `users`) aceitam o parâmetro opcional `q`.
- O termo é normalizado no core (`SearchTerm`): sem espaços nas pontas, minúsculas, sem acentos, com `%` e `_` escapados; dígitos à parte para CPF e CNPJ. Vazio equivale a sem busca.
- A comparação ignora maiúsculas e acentos com `TRANSLATE`, portável entre H2 e PostgreSQL, sem extensão.
- Termo acima de 100 caracteres: 400 `page.invalid.query`.
- Formato da `Page`, paginação e ordenação não mudam; `totalElements` reflete o resultado filtrado.

### Não-objetivos

- Busca por relevância ou texto completo, filtros estruturados e índices de busca.

## Capabilities

### New Capabilities

- `list-search`: parâmetro `q` nas listagens paginadas, campos pesquisáveis por recurso e normalização do termo.

### Modified Capabilities

<!-- Nenhuma. -->

## Impact

- Core: `SearchTerm`, `InvalidPageException.invalidQuery`, portas `findAll(PageRequest, SearchTerm)`.
- Infra: `Specification` com `TRANSLATE` e `left join` nos 8 adaptadores; `JpaSpecificationExecutor` nos repositórios JPA; parâmetro `q` nos controllers.
- Aplicação: use cases com `findAll(PageRequest, String)`.
- README e OpenAPI. Sem migração.
