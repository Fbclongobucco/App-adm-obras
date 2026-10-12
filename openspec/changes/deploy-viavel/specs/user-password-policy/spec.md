## Purpose

Definir a regra mínima que toda senha de usuário deve cumprir, avaliada sobre a senha informada e aplicada da mesma forma em todo ponto do sistema que define uma senha.

## ADDED Requirements

### Requirement: Password has at least 6 characters

Toda senha definida para um usuário SHALL ter no mínimo 6 caracteres, de qualquer tipo. A regra SHALL ser avaliada sobre a senha informada, antes de qualquer transformação para armazenamento.

#### Scenario: Six-character password is accepted

- **WHEN** um usuário é criado com senha de exatamente 6 caracteres
- **THEN** o usuário é criado
- **AND** é possível autenticar com essa senha

#### Scenario: Five-character password is rejected on creation

- **WHEN** um ADMIN cria um usuário com senha de 5 caracteres
- **THEN** a requisição é rejeitada com erro de validação (400)
- **AND** nenhum usuário é criado

#### Scenario: Short password is rejected on update

- **WHEN** um ADMIN atualiza um usuário enviando senha nova de menos de 6 caracteres
- **THEN** a requisição é rejeitada com erro de validação (400)
- **AND** a senha anterior do usuário continua válida

#### Scenario: Update without password keeps the current one

- **WHEN** um ADMIN atualiza um usuário sem enviar senha
- **THEN** a senha atual do usuário é mantida

### Requirement: Missing password is rejected on user creation

A criação de usuário SHALL exigir senha. Senha ausente ou só com espaços SHALL ser rejeitada com erro de validação.

#### Scenario: Blank password is rejected

- **WHEN** um ADMIN cria um usuário com senha vazia
- **THEN** a requisição é rejeitada com erro de validação (400)

### Requirement: Existing stored passwords are not invalidated

A aplicação da regra SHALL NOT invalidar nem alterar senhas já armazenadas. A regra vale apenas quando uma senha é definida.

#### Scenario: Previously stored user still authenticates

- **WHEN** um usuário criado antes da regra, com senha de menos de 6 caracteres, tenta autenticar
- **THEN** a autenticação funciona normalmente
