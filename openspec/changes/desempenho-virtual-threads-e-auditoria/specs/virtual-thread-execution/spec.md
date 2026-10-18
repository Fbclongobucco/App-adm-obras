## Purpose

Define como a aplicação atende requisições HTTP e executa tarefas agendadas em virtual threads, com chave para desligar o recurso e comparar o desempenho nos dois modos.

## ADDED Requirements

### Requirement: Requests are served on virtual threads by default

Com a configuração padrão, em todos os perfis, cada requisição HTTP SHALL ser atendida em uma virtual thread.

#### Scenario: Request runs on a virtual thread

- **WHEN** a aplicação sobe sem configuração explícita de threads e recebe uma requisição autenticada
- **THEN** o código que atende a requisição executa em uma virtual thread

### Requirement: Scheduled tasks run on virtual threads

Com virtual threads ligadas, as tarefas agendadas (incluindo a limpeza de refresh tokens expirados) SHALL executar em virtual threads e manter o mesmo efeito de antes.

#### Scenario: Cleanup keeps working

- **WHEN** a limpeza de refresh tokens é disparada com virtual threads ligadas
- **THEN** os tokens expirados são removidos e os válidos permanecem

### Requirement: Virtual threads can be switched off by environment

O uso de virtual threads SHALL ser desligável por variável de ambiente, sem recompilar. Desligado, o atendimento SHALL voltar ao pool de threads de plataforma, sem mudança de resultado nas respostas da API.

#### Scenario: Switched off

- **WHEN** a aplicação sobe com virtual threads desligadas por variável de ambiente
- **THEN** as requisições são atendidas em threads de plataforma
- **AND** as respostas da API são as mesmas do modo ligado

### Requirement: Connection pool size is configurable

O número máximo de conexões com o banco SHALL ser configurável por variável de ambiente. Sem a variável, SHALL valer o padrão do pool de conexões.

#### Scenario: Pool size from environment

- **WHEN** a aplicação sobe com o tamanho máximo do pool definido por variável de ambiente
- **THEN** o pool de conexões usa esse limite

### Requirement: Behavior is unchanged under concurrency

Com virtual threads ligadas, requisições simultâneas SHALL produzir os mesmos resultados que produziriam em sequência, incluindo a rotação de refresh token com a janela de tolerância ao reuso já existente.

#### Scenario: Concurrent refresh within grace window

- **WHEN** duas requisições de refresh com o mesmo token chegam ao mesmo tempo, dentro da janela de tolerância
- **THEN** as duas recebem sucessores da mesma família e a sessão não é revogada
