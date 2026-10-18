## Purpose

Garante que o número de consultas ao banco de cada leitura e escrita da API não cresça com o tamanho da página nem com o tamanho das coleções do registro, e que os índices do schema sejam sustentados por medição.

## ADDED Requirements

### Requirement: Listing query count does not grow with page size

Para cada listagem paginada da API (obras, clientes, colaboradores, centros de custo, hospedagens, refeições, restaurantes e usuários), o número de consultas ao banco SHALL ser o mesmo para páginas de tamanhos diferentes, desde que cada página tenha registros com o mesmo formato de vínculos.

#### Scenario: Larger page issues the same number of queries

- **WHEN** a mesma listagem é atendida com tamanho de página 5 e depois com tamanho de página 50, com pelo menos 50 registros no banco
- **THEN** o número de consultas ao banco é o mesmo nos dois atendimentos

#### Scenario: Listing with search keeps the bound

- **WHEN** a listagem é atendida com o parâmetro de busca `q`
- **THEN** o número de consultas também não cresce com o tamanho da página

### Requirement: Detail read is bounded by collection size

A leitura de detalhe de uma obra, hospedagem, refeição ou cliente SHALL executar um número de consultas que não cresce com a quantidade de colaboradores, refeições, restaurantes, hospedagens, períodos, ajustes ou obras vinculados.

#### Scenario: Project with many links

- **WHEN** o detalhe é lido para uma obra com 2 colaboradores e depois para uma obra com 30 colaboradores
- **THEN** o número de consultas é o mesmo nos dois casos

### Requirement: Write of links is bounded

Gravar uma obra ou uma hospedagem com uma lista de colaboradores (e, para a obra, de hospedagens) SHALL executar um número de consultas de leitura que não cresce com o tamanho dessa lista.

#### Scenario: Project saved with many employees

- **WHEN** uma obra é gravada com 2 colaboradores e outra com 30 colaboradores
- **THEN** o número de consultas de leitura dos vínculos é o mesmo nos dois casos

### Requirement: Responses keep their content

As otimizações de consulta SHALL preservar o conteúdo das respostas. Listagem e detalhe do mesmo registro SHALL apresentar os mesmos dados, e a resposta SHALL ser serializada sem erro de dado não carregado.

#### Scenario: Listing and detail agree

- **WHEN** uma obra com colaboradores, refeições, restaurantes e hospedagens é lida pela listagem e pelo detalhe
- **THEN** as duas respostas apresentam os mesmos dados

#### Scenario: No lazy-loading failure

- **WHEN** qualquer listagem ou detalhe é serializado
- **THEN** a resposta é 200 e não há falha por dado não carregado

### Requirement: Paging limits are enforced before querying

A validação de paginação existente SHALL continuar valendo. Tamanho de página fora do limite ou índice negativo SHALL ser rejeitado com 400, sem consulta ao banco.

#### Scenario: Oversized page

- **WHEN** uma listagem é requisitada com tamanho de página acima do limite
- **THEN** a resposta é 400 e nenhuma consulta é executada

### Requirement: Indexes are backed by measurement

Todo índice que este change adicionar a uma tabela já existente SHALL ter registro da consulta que ele atende e do plano de execução em PostgreSQL antes e depois, com massa de dados. Índice sem melhora no plano medido SHALL NOT ser adicionado. Os índices das tabelas novas de auditoria seguem os filtros definidos em `change-audit-log` e `login-audit-log`. As migrações compartilhadas SHALL continuar válidas em PostgreSQL e em H2 com `MODE=PostgreSQL`.

#### Scenario: Index evidence recorded

- **WHEN** uma migração deste change cria um índice de desempenho
- **THEN** o relatório de desempenho traz a consulta atendida e os planos antes e depois

#### Scenario: Migrations run on both databases

- **WHEN** as migrações são aplicadas em H2 com `MODE=PostgreSQL` e em PostgreSQL
- **THEN** as duas aplicações terminam sem erro
