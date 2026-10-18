# performance-testing Specification

## Purpose

Oferece meios reproduzíveis de medir o desempenho da API: contagem automatizada de consultas por endpoint, que bloqueia regressão de N+1, e teste de carga com relatório comparativo que embasa as propostas de melhoria.

## Requirements

### Requirement: Query count regression fails the test suite

A suíte de testes padrão SHALL incluir verificações de contagem de consultas para as listagens e detalhes cobertos por `persistence-query-efficiency`. Uma regressão que faça o número de consultas crescer com o tamanho da página SHALL fazer a suíte falhar, indicando o endpoint e as contagens observadas.

#### Scenario: N+1 reintroduced

- **WHEN** uma alteração volta a carregar uma coleção com uma consulta por registro da página
- **THEN** a suíte falha e a mensagem informa o endpoint e as duas contagens

### Requirement: Load test is reproducible with one command

O repositório SHALL conter um teste de carga executável com um único comando documentado, que cria a massa do cenário escolhido e executa o roteiro. O alvo SHALL ser escolhido no comando: a aplicação local (perfil `local`) ou o ambiente isolado, que sobe PostgreSQL, a aplicação e o gerador de carga em contêineres. O teste SHALL NOT exigir instalação de ferramenta além de Docker e do JDK do projeto.

#### Scenario: Run from a clean machine

- **WHEN** o comando documentado é executado com o alvo isolado, em máquina com Docker e sem o gerador de carga instalado
- **THEN** o ambiente sobe, a massa é criada, o roteiro roda e o resumo é gravado em arquivo

#### Scenario: Run against the local application

- **WHEN** a aplicação está rodando no perfil `local` e o comando é executado com o alvo local
- **THEN** a massa é criada e o roteiro roda contra essa aplicação

### Requirement: Seed data is created through the API

A massa de dados SHALL ser criada chamando os endpoints da API, autenticado como ADMIN, na ordem de dependência dos cadastros: centro de custo, cliente, colaborador, obra, restaurante, refeição, hospedagem e usuário. Os dados SHALL passar nas validações da aplicação, incluindo CPF e CNPJ válidos. A quantidade de cada tipo SHALL vir de um cenário nomeado, definido em arquivo editável. Endereço da API e credenciais SHALL ser parâmetros.

#### Scenario: Scenario volume

- **WHEN** a massa é criada com o cenário `pequeno`
- **THEN** as listagens da API mostram a quantidade de cada tipo definida para esse cenário

#### Scenario: Unknown scenario

- **WHEN** a massa é pedida com um cenário que não existe no arquivo
- **THEN** o script termina com erro antes de criar qualquer registro

#### Scenario: Rejected record

- **WHEN** a API rejeita um cadastro durante a criação da massa
- **THEN** o script termina com erro e informa o endpoint e a resposta recebida

### Requirement: Local profile runs on containerized PostgreSQL

O perfil `local` SHALL usar PostgreSQL em contêiner Docker, iniciado na subida da aplicação, com dados preservados entre reinícios. O perfil `test` SHALL continuar sem exigir Docker.

#### Scenario: Local start

- **WHEN** a aplicação sobe no perfil `local` em máquina com Docker
- **THEN** o contêiner do PostgreSQL é iniciado, as migrações são aplicadas e o login do admin do bootstrap responde 200

#### Scenario: Restart keeps data

- **WHEN** a aplicação local é reiniciada depois de criada a massa
- **THEN** os registros criados continuam disponíveis

#### Scenario: Test suite without Docker

- **WHEN** `mvnw test` roda em máquina sem Docker
- **THEN** a suíte executa em H2, como antes

### Requirement: Load scenario covers authenticated reads and writes

O roteiro de carga SHALL autenticar pelo fluxo real de login e exercitar as listagens paginadas, os detalhes, a busca `q` e pelo menos uma escrita de cadastro, com número de usuários virtuais e duração parametrizáveis.

#### Scenario: Parameters change the load

- **WHEN** o teste é executado com outro número de usuários virtuais e outra duração
- **THEN** o resumo registra os valores usados

### Requirement: Comparison between thread modes

O teste de carga SHALL poder ser executado com virtual threads ligadas e desligadas, com a mesma massa e o mesmo roteiro, e o resumo SHALL registrar o modo de cada execução.

#### Scenario: Two runs, same conditions

- **WHEN** o teste é executado uma vez em cada modo
- **THEN** os dois resumos têm a mesma massa, o mesmo roteiro e o modo identificado

### Requirement: Results report with measured values only

Cada execução SHALL produzir latência p50, p95 e p99, vazão e taxa de erro por grupo de endpoint. O relatório versionado SHALL conter somente valores medidos, com data, versão da aplicação, alvo (local ou isolado), cenário de volume, parâmetros de carga e recursos da máquina. Os volumes dos cenários SHALL ser declarados como nominais, não como volume de produção. Melhoria proposta sem medição SHALL ser marcada como hipótese.

#### Scenario: Report entry

- **WHEN** uma execução é registrada no relatório
- **THEN** a entrada traz as métricas por grupo de endpoint e as condições da execução, incluindo alvo e cenário

#### Scenario: Comparable runs

- **WHEN** duas execuções são comparadas no relatório
- **THEN** elas têm o mesmo alvo, o mesmo cenário e a mesma máquina
