## Purpose

Garante que duplicidades e exclusões bloqueadas por vínculo sejam respondidas como conflito com código de erro específico, em vez de erro interno, e que nenhuma delas altere dados.

## ADDED Requirements

### Requirement: Duplicate unique values respond 409

Criação ou alteração que repita valor único de outro registro SHALL responder 409 com o formato de erro padrão e `errorCode` específico, sem gravar nada. Códigos: `project.duplicate.os`, `client.duplicate.email`, `employee.duplicate.email`, `employee.duplicate.cpf`, `user.duplicate.email`, `meal.duplicate.type`.

#### Scenario: Duplicate OS on create

- **WHEN** uma obra é criada com a OS de outra obra existente
- **THEN** a resposta é 409 com `errorCode` `project.duplicate.os`
- **AND** nenhuma obra nova é gravada

#### Scenario: Duplicate CPF on update

- **WHEN** um colaborador é alterado para o CPF de outro colaborador
- **THEN** a resposta é 409 com `errorCode` `employee.duplicate.cpf`
- **AND** o colaborador mantém os dados anteriores

#### Scenario: Duplicate user email on update

- **WHEN** um usuário é alterado para o e-mail de outro usuário
- **THEN** a resposta é 409 com `errorCode` `user.duplicate.email`

### Requirement: Keeping the own unique value is not a conflict

Alterar um registro mantendo o próprio valor único SHALL ser aceito. A comparação de e-mail SHALL ignorar maiúsculas e minúsculas; a de CPF e OS SHALL usar o valor normalizado.

#### Scenario: Update with unchanged OS

- **WHEN** uma obra é alterada sem mudar a própria OS
- **THEN** a alteração é aceita

#### Scenario: Email differing only by case

- **WHEN** um cliente é criado com o e-mail de outro cliente escrito em maiúsculas
- **THEN** a resposta é 409 com `errorCode` `client.duplicate.email`

### Requirement: Deleting a referenced record responds 409

Exclusão de registro com dependentes SHALL responder 409 com `errorCode` `<recurso>.in_use`, sem apagar nada. Bloqueiam: centro de custo com colaboradores ou obras; cliente com obras; obra com restaurantes, refeições ou hospedagens; colaborador em obra ou hospedagem; restaurante com refeições.

#### Scenario: Cost center with employees

- **WHEN** um centro de custo com colaboradores é excluído
- **THEN** a resposta é 409 com `errorCode` `cost_center.in_use`
- **AND** o centro de custo continua existindo

#### Scenario: Project with restaurants

- **WHEN** uma obra com restaurantes é excluída
- **THEN** a resposta é 409 com `errorCode` `project.in_use`

#### Scenario: Record without dependents is deleted

- **WHEN** um cliente sem obras é excluído
- **THEN** a resposta é 204 e o cliente deixa de existir

### Requirement: Conflict message names the dependency

Na exclusão bloqueada, a mensagem SHALL informar o tipo de vínculo que impede a exclusão.

#### Scenario: Message for blocked employee deletion

- **WHEN** um colaborador vinculado a uma obra é excluído
- **THEN** a mensagem do erro menciona o vínculo com obra

### Requirement: Integrity violations never respond 500

Violação de integridade do banco não detectada antes pela aplicação SHALL responder 409 com `errorCode` `data.integrity_violation`, sem expor detalhes do banco na mensagem.

#### Scenario: Integrity violation outside application checks

- **WHEN** uma gravação viola uma restrição do banco que a aplicação não verificou antes
- **THEN** a resposta é 409 com `errorCode` `data.integrity_violation`
- **AND** a mensagem não contém nome de tabela, coluna ou restrição
