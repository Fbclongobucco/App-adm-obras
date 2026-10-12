## Purpose

Permite cadastrar clientes e colaboradores sem e-mail, mantendo a validação de formato e a unicidade quando o e-mail é informado.

## ADDED Requirements

### Requirement: Client and employee email is optional

O e-mail de cliente e de colaborador SHALL ser opcional. Valor `null` ou em branco SHALL ser gravado e devolvido como `null`. O e-mail de usuário (`User`) SHALL continuar obrigatório.

#### Scenario: Client without email

- **WHEN** um cliente é criado com `email` vazio
- **THEN** o cliente é gravado e a resposta traz `email` `null`

#### Scenario: Employee without email

- **WHEN** um colaborador é criado sem o campo `email`
- **THEN** o colaborador é gravado e a resposta traz `email` `null`

### Requirement: Informed email is validated

Quando informado, o e-mail SHALL ter formato válido. E-mail inválido SHALL responder 400 com `client.invalid.email` ou `employee.invalid.email`.

#### Scenario: Invalid client email

- **WHEN** um cliente é criado com `email` "cliente@"
- **THEN** a resposta é 400 com `errorCode` `client.invalid.email`

#### Scenario: Invalid employee email

- **WHEN** um colaborador é criado com `email` "montador@"
- **THEN** a resposta é 400 com `errorCode` `employee.invalid.email`

### Requirement: Informed email is unique

Um e-mail informado SHALL ser único entre os clientes e entre os colaboradores. Duplicidade SHALL responder 409 com `client.duplicate.email` ou `employee.duplicate.email`. Registros sem e-mail SHALL NOT conflitar entre si, na aplicação e no banco.

#### Scenario: Duplicate client email

- **WHEN** um cliente é criado com o e-mail de outro cliente
- **THEN** a resposta é 409 com `errorCode` `client.duplicate.email`

#### Scenario: Several clients without email

- **WHEN** dois clientes são criados sem e-mail
- **THEN** os dois são gravados

#### Scenario: Several employees without email

- **WHEN** dois colaboradores são criados sem e-mail
- **THEN** os dois são gravados
