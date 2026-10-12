# Tasks

Referência do contrato: `App-adm-obras-front/openspec/changes/busca-nas-listagens/design.md` (D1 a D3).

## 1. Core

- [x] 1.1 `SearchTerm` e `InvalidPageException.invalidQuery`; portas com `findAll(PageRequest, SearchTerm)`. Verificar por `SearchTermTest`: vazio, espaços, "José Antônio" → "jose antonio", escape de `%` e `_`, `digits()` de "529.982", limite de 100.

## 2. Infra e API

- [x] 2.1 `SearchSpecification`, `JpaSpecificationExecutor` nos 8 repositórios JPA, adaptadores, use cases e controllers com `q`. Verificar por `ListSearchIntegrationTest`: um campo por recurso, campo de relação, CPF formatado, `totalElements` filtrado, registro sem endereço e 400 `page.invalid.query`.
- [x] 2.2 README e descrições OpenAPI com `q`. Verificar no README e em `OpenApiDocumentationIntegrationTest`.

## 3. Integração

- [x] 3.1 `./mvnw test` sem falhas e `openspec validate busca-nas-listagens --strict` válido.
