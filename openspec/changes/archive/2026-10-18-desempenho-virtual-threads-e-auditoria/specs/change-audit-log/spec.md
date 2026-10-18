## Purpose

Registra quem criou, alterou ou excluiu cada registro de negócio e quando, e permite ao administrador consultar esse histórico pela API. Não guarda os valores alterados.

## ADDED Requirements

### Requirement: Every business write produces an audit event

Cada criação, alteração ou exclusão confirmada de obra, colaborador, centro de custo, cliente, hospedagem, período de hospedagem, refeição, ajuste de refeição, restaurante e usuário SHALL gerar um evento de auditoria com: tipo do registro, id do registro, ação (`CREATE`, `UPDATE` ou `DELETE`), instante e autor.

Tipos de registro: `PROJECT`, `EMPLOYEE`, `COST_CENTER`, `CLIENT`, `ACCOMMODATION`, `ACCOMMODATION_PERIOD`, `MEAL`, `MEAL_ADJUSTMENT`, `RESTAURANT`, `USER`.

#### Scenario: Create

- **WHEN** um ADMIN cria um cliente
- **THEN** existe um evento `CLIENT`/`CREATE` com o id do cliente, o ADMIN como autor e o instante da gravação

#### Scenario: Update

- **WHEN** um ADMIN altera uma obra
- **THEN** existe um evento `PROJECT`/`UPDATE` com o id da obra e o ADMIN como autor

#### Scenario: Delete

- **WHEN** um ADMIN exclui um restaurante
- **THEN** existe um evento `RESTAURANT`/`DELETE` com o id do restaurante, que continua consultável depois da exclusão

#### Scenario: Child record

- **WHEN** um ADMIN renova uma hospedagem, criando um período
- **THEN** existe um evento `ACCOMMODATION_PERIOD`/`CREATE` com o id do período

### Requirement: Event author identifies the user

O autor SHALL ser o usuário autenticado da requisição, registrado por id, nome e e-mail vigentes no instante do evento. Alterações posteriores ou exclusão do usuário SHALL NOT alterar eventos já gravados. Gravação sem usuário autenticado (rotina interna ou bootstrap) SHALL ter o autor `SYSTEM`, sem id.

#### Scenario: Author renamed later

- **WHEN** o usuário autor de um evento muda de nome depois
- **THEN** o evento mantém o nome do instante da gravação

#### Scenario: Bootstrap

- **WHEN** o bootstrap cria o usuário administrador na subida da aplicação
- **THEN** o evento `USER`/`CREATE` tem autor `SYSTEM`

### Requirement: Event is atomic with the write

O evento SHALL ser gravado na mesma transação da alteração. Alteração rejeitada ou desfeita SHALL NOT deixar evento. Falha ao gravar o evento SHALL desfazer a alteração.

#### Scenario: Rejected write

- **WHEN** uma alteração é rejeitada por conflito (409) ou validação (400)
- **THEN** nenhum evento é gravado para ela

#### Scenario: No-op request

- **WHEN** uma requisição não grava nada no banco
- **THEN** nenhum evento é gravado

### Requirement: Event stores no field values

O evento SHALL NOT conter valores de campos do registro, nem antes nem depois da alteração. Em particular, SHALL NOT conter senha, hash de senha, CPF ou e-mail do registro alterado.

#### Scenario: User password change

- **WHEN** um ADMIN altera a senha de um usuário
- **THEN** o evento `USER`/`UPDATE` não contém senha nem hash

### Requirement: Administrators query the change log

`GET /api/audit/changes` SHALL devolver os eventos paginados, do mais recente para o mais antigo, com os parâmetros de paginação já usados pela API. Filtros opcionais e combináveis: `entityType`, `entityId`, `actorId`, `from` e `to` (instantes ISO-8601, `from` inclusivo, `to` exclusivo). Cada item SHALL trazer `id`, `entityType`, `entityId`, `action`, `occurredAt`, `actorId`, `actorName` e `actorEmail`.

#### Scenario: History of one record

- **WHEN** um ADMIN consulta com `entityType=PROJECT&entityId=<id>`
- **THEN** recebe só os eventos daquela obra, do mais recente para o mais antigo

#### Scenario: Changes by one user in a period

- **WHEN** um ADMIN consulta com `actorId`, `from` e `to`
- **THEN** recebe só os eventos daquele autor no intervalo

#### Scenario: Invalid filter

- **WHEN** `entityType` não é um tipo válido, um instante é malformado ou `from` é posterior a `to`
- **THEN** a resposta é 400 com `errorCode`

### Requirement: Only administrators read the change log

A consulta do log de alterações SHALL exigir papel ADMIN. Usuário OPERADOR SHALL receber 403; requisição sem token SHALL receber 401. Não SHALL existir endpoint para criar, alterar ou excluir eventos.

#### Scenario: Operator denied

- **WHEN** um OPERADOR chama `GET /api/audit/changes`
- **THEN** a resposta é 403
