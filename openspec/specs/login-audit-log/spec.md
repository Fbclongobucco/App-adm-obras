# login-audit-log Specification

## Purpose

Registra quem fez login com sucesso, quando, de qual IP e com qual user-agent, e permite ao administrador consultar esses registros pela API.

## Requirements

### Requirement: Successful login is recorded

Cada `POST /api/auth/login` com sucesso SHALL gerar um registro com: id do usuário, nome e e-mail vigentes no instante, instante do login, IP de origem e user-agent. Login rejeitado, refresh e logout SHALL NOT gerar registro.

#### Scenario: Successful login

- **WHEN** um usuário ativo faz login com credenciais válidas
- **THEN** existe um registro com o id dele, o instante do login, o IP e o user-agent da requisição

#### Scenario: Failed login

- **WHEN** um login é rejeitado com 401
- **THEN** nenhum registro é gravado

#### Scenario: Refresh and logout

- **WHEN** uma sessão é renovada por refresh ou encerrada por logout
- **THEN** nenhum registro de login é gravado

### Requirement: Missing request metadata does not block login

Ausência de user-agent SHALL NOT impedir o login; o campo fica vazio. User-agent maior que o limite de armazenamento SHALL ser truncado, não rejeitado.

#### Scenario: No user-agent

- **WHEN** o login é feito sem cabeçalho `User-Agent`
- **THEN** o login tem sucesso e o registro tem user-agent vazio

### Requirement: No session without its login record

O registro de login SHALL ser gravado antes da emissão da sessão. Se a gravação falhar, o login SHALL falhar e nenhuma sessão SHALL ser emitida.

#### Scenario: Record failure

- **WHEN** a gravação do registro de login falha
- **THEN** a resposta não é 200, nenhum refresh token é criado e nenhum access token é devolvido

### Requirement: Administrators query login records

`GET /api/audit/logins` SHALL devolver os registros paginados, do mais recente para o mais antigo, com os parâmetros de paginação já usados pela API. Filtros opcionais e combináveis: `userId`, `from` e `to` (instantes ISO-8601, `from` inclusivo, `to` exclusivo). Cada item SHALL trazer `id`, `userId`, `userName`, `userEmail`, `occurredAt`, `ipAddress` e `userAgent`. A consulta SHALL exigir papel ADMIN: OPERADOR recebe 403 e requisição sem token recebe 401.

#### Scenario: Logins of one user

- **WHEN** um ADMIN consulta com `userId=<id>`
- **THEN** recebe só os logins daquele usuário, do mais recente para o mais antigo

#### Scenario: Logins in a period

- **WHEN** um ADMIN consulta com `from` e `to`
- **THEN** recebe só os logins no intervalo

#### Scenario: Invalid period

- **WHEN** `from` é posterior a `to` ou um instante é malformado
- **THEN** a resposta é 400 com `errorCode`

#### Scenario: Operator denied

- **WHEN** um OPERADOR chama `GET /api/audit/logins`
- **THEN** a resposta é 403
