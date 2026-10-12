## Purpose

Define a regra de que um projeto possui no máximo uma refeição de cada tipo, e que essa unicidade é uma invariante do sistema protegida tanto pela aplicação quanto pelo banco de dados.

## ADDED Requirements

### Requirement: A project holds at most one meal of each type

Um projeto SHALL ter no máximo uma refeição do tipo almoço e no máximo uma refeição do tipo janta. O sistema SHALL rejeitar a criação ou vinculação de uma segunda refeição do mesmo tipo a um mesmo projeto.

#### Scenario: First lunch and dinner are accepted

- **WHEN** um projeto recebe uma refeição de almoço e uma de janta, cada uma uma única vez
- **THEN** ambas são aceitas e vinculadas ao projeto

#### Scenario: Second lunch on the same project is rejected

- **WHEN** um projeto que já possui uma refeição de almoço recebe outra refeição do tipo almoço
- **THEN** a requisição é rejeitada com erro de validação
- **AND** o projeto mantém apenas a refeição de almoço original

#### Scenario: Second dinner on the same project is rejected

- **WHEN** um projeto que já possui uma refeição de janta recebe outra refeição do tipo janta
- **THEN** a requisição é rejeitada com erro de validação
- **AND** o projeto mantém apenas a refeição de janta original

#### Scenario: Lunch and dinner coexist for the same project

- **WHEN** um projeto possui uma refeição de almoço e uma de janta
- **THEN** a coexistência dos dois tipos é permitida

#### Scenario: Meals of the same type coexist across different projects

- **WHEN** dois projetos distintos recebem refeições do mesmo tipo
- **THEN** ambas as vinculações são aceitas

### Requirement: Uniqueness is enforced by the database

O banco de dados SHALL rejeitar a persistência de duas refeições do mesmo tipo vinculadas ao mesmo projeto, independentemente de qualquer verificação feita pela aplicação. A rejeição do banco SHALL ser traduzida para a mesma resposta de erro de validação apresentada quando a aplicação detecta a violação.

#### Scenario: Duplicate persisted outside the application is rejected

- **WHEN** duas refeições do mesmo tipo são persistidas para o mesmo projeto sem passar pela verificação da aplicação
- **THEN** o banco de dados rejeita a segunda gravação

#### Scenario: Database rejection surfaces as a validation error

- **WHEN** uma gravação viola a unicidade no banco de dados
- **THEN** a requisição recebe erro de validação, e não erro interno de servidor

### Requirement: Replacing a project's meals is evaluated as a whole

Quando a lista de refeições de um projeto for substituída, a unicidade SHALL ser avaliada sobre o conjunto resultante, e não meal a meal.

#### Scenario: Swapping lunch and dinner within one request is accepted

- **WHEN** um projeto que tem almoço e janta recebe, numa única alteração, um conjunto final com exatamente um almoço e uma janta
- **THEN** a alteração é aceita

#### Scenario: Final set with two lunches is rejected

- **WHEN** um projeto recebe, numa única alteração, um conjunto final com duas refeições de almoço
- **THEN** a requisição é rejeitada com erro de validação
- **AND** o conjunto de refeições do projeto permanece o anterior

### Requirement: Meal reads for a project are unambiguous

Uma leitura de projeto SHALL retornar no máximo uma refeição de cada tipo, de forma determinística, sem depender da ordem arbitrária do banco.

#### Scenario: Reading a project returns its meals by type

- **WHEN** um projeto com uma refeição de almoço e uma de janta é lido
- **THEN** a resposta associa o almoço e a janta aos campos correspondentes, sem duplicidade

#### Scenario: Reading a project with only a lunch returns no dinner

- **WHEN** um projeto que possui apenas uma refeição de almoço é lido
- **THEN** a resposta traz o almoço associado e a janta ausente, e não um erro