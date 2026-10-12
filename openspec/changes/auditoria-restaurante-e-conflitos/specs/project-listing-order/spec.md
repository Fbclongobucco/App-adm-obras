## Purpose

Permite que a listagem paginada de obras seja ordenada por campos permitidos, como a data de alteração, de forma determinística entre páginas.

## ADDED Requirements

### Requirement: Project listing accepts a sort parameter

`GET /api/projects` SHALL aceitar o parâmetro `sort` no formato `campo,direção`, com direção `asc` ou `desc`. Sem direção, a ordem SHALL ser `asc`. Campos permitidos: `updatedAt`, `createdAt`, `os`, `startDate`.

#### Scenario: Most recently updated first

- **WHEN** a listagem é pedida com `sort=updatedAt,desc&size=10`
- **THEN** a resposta traz até 10 obras, da alteração mais recente para a mais antiga

#### Scenario: Direction defaults to ascending

- **WHEN** a listagem é pedida com `sort=os`
- **THEN** as obras vêm em ordem crescente de OS

### Requirement: Unsupported sort is rejected

Campo fora da lista permitida ou direção diferente de `asc`/`desc` SHALL ser rejeitado com 400 e `errorCode` `page.invalid.sort`, antes de qualquer consulta.

#### Scenario: Unknown field is rejected

- **WHEN** a listagem é pedida com `sort=totalPrice,desc`
- **THEN** a resposta é 400 com `errorCode` `page.invalid.sort`

#### Scenario: Unknown direction is rejected

- **WHEN** a listagem é pedida com `sort=updatedAt,up`
- **THEN** a resposta é 400 com `errorCode` `page.invalid.sort`

### Requirement: Sorted pages are deterministic

A ordenação SHALL ser estável entre páginas: obras com o mesmo valor no campo ordenado SHALL ter desempate fixo, sem repetir nem pular registros entre páginas consecutivas.

#### Scenario: Ties do not repeat across pages

- **WHEN** várias obras têm o mesmo `updatedAt` e a listagem é percorrida página a página
- **THEN** cada obra aparece exatamente uma vez no conjunto das páginas

### Requirement: Listing without sort keeps working

Sem o parâmetro `sort`, `GET /api/projects` SHALL continuar respondendo com a mesma estrutura de página e as mesmas validações de `page` e `size`.

#### Scenario: Request without sort

- **WHEN** a listagem é pedida só com `page` e `size`
- **THEN** a resposta é 200 com a estrutura de página atual
