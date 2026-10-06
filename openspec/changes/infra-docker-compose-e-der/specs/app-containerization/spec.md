## Purpose

Definir como a aplicação é empacotada e iniciada como contêiner, e qual é sua relação com o banco de dados, que é externo e gerenciado fora deste repositório.

## ADDED Requirements

### Requirement: The application runs as a container

O sistema SHALL fornecer uma imagem de contêiner que executa a aplicação web completa, e essa imagem SHALL ser a forma suportada de rodar a aplicação por orquestração local.

#### Scenario: Compose brings the application up

- **WHEN** a orquestração é iniciada a partir do arquivo de definição da raiz
- **THEN** um contêiner da aplicação é criado e iniciado
- **AND** a aplicação passa a atender requisições na porta exposta

#### Scenario: Compose down removes the application

- **WHEN** a orquestração é encerrada com remoção de contêineres
- **THEN** nenhum contêiner da aplicação permanece em execução

#### Scenario: Application restarts without losing configuration

- **WHEN** a aplicação é reiniciada pela orquestração
- **THEN** ela volta a atender requisições com a mesma configuração

### Requirement: The runtime image carries no build tooling

A imagem de execução SHALL conter apenas o runtime e o artefato da aplicação. Ferramentas de construção, fontes e cache de build SHALL NOT estar presentes na imagem final.

#### Scenario: Build tooling is absent from the runtime image

- **WHEN** a imagem de execução é inspecionada
- **THEN** não há ferramenta de construção nem fontes do projeto nela

#### Scenario: Build is reproducible from a clean context

- **WHEN** a imagem é construída a partir do repositório sem artefatos prévios
- **THEN** a construção conclui sem depender de estado deixado na máquina

### Requirement: The runtime image does not run as root

O processo da aplicação dentro do contêiner SHALL executar com usuário sem privilégios de administrador.

#### Scenario: Process identity is unprivileged

- **WHEN** a aplicação em execução é inspecionada de dentro do contêiner
- **THEN** o processo não é o usuário root

### Requirement: The database is external and not orchestrated here

A orquestração local SHALL NOT definir, criar ou gerenciar serviço de banco de dados. A aplicação SHALL obter a conexão ao banco externo exclusivamente por configuração. O Compose SHALL NOT subir banco algum, e a ausência de serviço de banco no Compose SHALL ser intencional, não um descuido.

#### Scenario: Compose defines no database service

- **WHEN** a definição de orquestração é inspecionada
- **THEN** não há serviço de banco de dados definido
- **AND** não há volume de dados de banco definido

#### Scenario: Application connects to the configured external database

- **WHEN** a aplicação sobe apontada para um banco PostgreSQL externo e alcançável
- **THEN** ela se conecta a esse banco e opera sobre ele

#### Scenario: Unreachable database surfaces as a startup failure

- **WHEN** a aplicação soba sem conseguir alcançar o banco configurado
- **THEN** a inicialização falha com erro que identifica a falha de conexão

### Requirement: The application does not attempt to orchestrate itself

O processo da aplicação SHALL NOT iniciar contêineres a partir da definição de orquestração do próprio projeto. Isso vale para todos os perfis de execução, incluindo o de produção.

#### Scenario: Production profile does not spawn containers

- **WHEN** a aplicação sobe com o perfil de produção a partir de um ambiente onde a orquestração está disponível
- **THEN** ela não tenta iniciar nenhum serviço de contêiner

#### Scenario: Local profile remains unaffected

- **WHEN** a aplicação sobe com o perfil de desenvolvimento
- **THEN** o comportamento de orquestração automática permanece desativado, como já é hoje

### Requirement: The application listens on a non-privileged port and reports its own readiness

O contêiner SHALL expor a aplicação em porta não privilegiada e SHALL declarar um mecanismo pelo qual o estado de prontidão da aplicação pode ser verificado externamente.

#### Scenario: Port is unprivileged

- **WHEN** o contêiner da aplicação é inspecionado
- **THEN** a aplicação escuta em porta superior a 1024

#### Scenario: Readiness is verifiable

- **WHEN** a verificação de prontidão declarada pelo contêiner é executada contra a aplicação em bom estado
- **THEN** ela reporta a aplicação como pronta