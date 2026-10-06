## Purpose

Define o custo de consulta das leituras de projeto, especialmente a listagem paginada, garantindo que o atendimento de uma página de projetos não execute um número de consultas que cresça com o tamanho da página.

## ADDED Requirements

### Requirement: Query count does not grow with page size

O número de consultas ao banco executadas para atender uma listagem paginada de projetos SHALL permanecer constante em relação ao tamanho da página. Servir uma página com o mesmo número de itens SHALL executar o mesmo número de consultas, independentemente de esse número.

#### Scenario: Larger page does not issue more queries

- **WHEN** uma listagem de projetos é atendida com tamanho de página pequeno e, em seguida, com tamanho de página muito maior
- **THEN** o número de consultas ao banco é o mesmo nos dois casos

#### Scenario: Repeated listing is stable

- **WHEN** a mesma listagem paginada é atendida repetidamente
- **THEN** o número de consultas por atendimento não se acumula entre requisições

### Requirement: Listing returns fully populated projects

Uma listagem paginada de projetos SHALL devolver cada projeto com seus funcionários, refeições e acomodações preenchidos, sem depender de acesso adiado a dados depois de concluída a consulta que os originou. A resposta SHALL ser indistinguível, quanto ao conteúdo, de uma leitura de detalhe do mesmo projeto.

#### Scenario: Listed project includes its collections

- **WHEN** um projeto possui funcionários, uma refeição de almoço e uma acomodação, e é devolvido por listagem paginada
- **THEN** a resposta inclui funcionários, almoço e acomodação do projeto

#### Scenario: Listing does not fail after the query completes

- **WHEN** uma listagem paginada é atendida
- **THEN** a serialização da resposta não falha por dados não carregados

#### Scenario: Listing and detail agree

- **WHEN** o mesmo projeto é obtido por listagem paginada e por leitura de detalhe
- **THEN** os dois resultados apresentam os mesmos dados

### Requirement: Detail read is also bounded

A leitura de detalhe de um projeto SHALL executar um número de consultas que não cresce com a quantidade de funcionários, refeições ou acomodações do projeto.

#### Scenario: Detail cost is independent of collection size

- **WHEN** a leitura de detalhe é executada para projetos com coleções de tamanhos diferentes
- **THEN** o número de consultas é equivalente em ambos os casos

### Requirement: Existing paging limits remain enforced

A validação de paginação existente SHALL continuar valendo para as leituras otimizadas. Tamanho de página fora dos limites permitidos SHALL ser rejeitado, e a rejeição SHALL ocorrer antes de qualquer consulta ao banco.

#### Scenario: Oversized page is rejected without querying

- **WHEN** uma listagem é requisitada com tamanho de página acima do limite
- **THEN** a requisição é rejeitada com erro de validação

#### Scenario: Non-positive page index is rejected

- **WHEN** uma listagem é requisitada com índice de página negativo
- **THEN** a requisição é rejeitada com erro de validação