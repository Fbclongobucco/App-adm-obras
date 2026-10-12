## Purpose

Permite saber quando cada registro de negócio foi criado e quando foi alterado pela última vez, com valores definidos pelo servidor e expostos pela API.

## ADDED Requirements

### Requirement: Business records expose creation and update timestamps

Toda resposta de obra, colaborador, centro de custo, cliente, hospedagem, refeição, restaurante e usuário SHALL incluir `createdAt` e `updatedAt`, em formato de data e hora ISO-8601 com fuso UTC.

#### Scenario: Created record carries both timestamps

- **WHEN** um registro é criado pela API
- **THEN** a resposta traz `createdAt` e `updatedAt` preenchidos
- **AND** os dois valores são iguais

#### Scenario: Listing and detail return the same timestamps

- **WHEN** um mesmo registro é lido pela listagem e pelo detalhe
- **THEN** `createdAt` e `updatedAt` são iguais nas duas respostas

### Requirement: Timestamps are set by the server

Os valores de `createdAt` e `updatedAt` SHALL ser definidos pelo servidor. Valores enviados pelo cliente na requisição SHALL ser ignorados.

#### Scenario: Client-supplied timestamps are ignored

- **WHEN** uma requisição de criação ou alteração envia `createdAt` ou `updatedAt`
- **THEN** a resposta traz os valores definidos pelo servidor, e não os enviados

### Requirement: Creation timestamp never changes

`createdAt` SHALL permanecer o mesmo após qualquer alteração do registro.

#### Scenario: Update keeps the creation timestamp

- **WHEN** um registro existente é alterado com sucesso
- **THEN** `createdAt` mantém o valor original

### Requirement: Update timestamp advances on every successful write

`updatedAt` SHALL receber o instante da gravação a cada alteração bem-sucedida feita pela API do próprio recurso. Uma alteração rejeitada SHALL NOT mudar `updatedAt`.

#### Scenario: Successful update advances the timestamp

- **WHEN** um registro é alterado com sucesso
- **THEN** `updatedAt` passa a ser posterior ao valor anterior

#### Scenario: Rejected update keeps the timestamp

- **WHEN** uma alteração é rejeitada por validação ou conflito
- **THEN** `updatedAt` permanece com o valor anterior

### Requirement: Project timestamp reflects only project writes

`updatedAt` de uma obra SHALL mudar apenas por alteração da própria obra. Mudanças em restaurantes, refeições ou hospedagens vinculadas, e o recálculo do total da obra, SHALL NOT alterá-lo.

#### Scenario: Meal change does not touch project timestamp

- **WHEN** uma refeição de uma obra é criada, alterada ou excluída
- **THEN** `updatedAt` da obra permanece o mesmo

### Requirement: Existing records receive a timestamp on migration

Registros que já existiam antes da introdução das datas SHALL receber `createdAt` e `updatedAt` preenchidos, nunca nulos.

#### Scenario: Pre-existing record is readable with timestamps

- **WHEN** um registro gravado antes da migração é lido
- **THEN** `createdAt` e `updatedAt` estão preenchidos
