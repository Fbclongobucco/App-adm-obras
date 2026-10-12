## Purpose

Permitir que uma instalação nova, inclusive de produção, receba seu primeiro usuário administrador de forma controlada por configuração, sem credencial padrão embutida no artefato e sem alterar usuários já existentes.

## ADDED Requirements

### Requirement: Admin bootstrap is opt-in in every environment

O provisionamento do administrador inicial SHALL ficar desligado por padrão e SHALL poder ser habilitado por configuração em qualquer ambiente, inclusive produção.

#### Scenario: Bootstrap disabled by default

- **WHEN** a aplicação sobe sem configuração de bootstrap
- **THEN** nenhum usuário é criado na inicialização

#### Scenario: Bootstrap enabled in production

- **WHEN** a aplicação sobe no perfil de produção com o bootstrap habilitado, e-mail e senha informados, e nenhum usuário com esse e-mail
- **THEN** um usuário ativo com perfil ADMIN é criado com esse e-mail
- **AND** é possível autenticar com o e-mail e a senha informados

### Requirement: No built-in admin credential outside the test profile

Fora do perfil de teste, o sistema SHALL NOT assumir e-mail ou senha padrão para o administrador inicial. Esses valores SHALL vir exclusivamente da configuração da instalação.

#### Scenario: Enabled without credentials does not fall back to defaults

- **WHEN** o bootstrap é habilitado fora do perfil de teste sem e-mail ou sem senha configurados
- **THEN** nenhum usuário é criado com credencial padrão

#### Scenario: Test profile keeps its fixed admin

- **WHEN** a aplicação sobe no perfil de teste
- **THEN** o administrador de teste `admin@admin.com` está disponível para autenticação

### Requirement: Incomplete bootstrap configuration fails at startup

Com o bootstrap habilitado, a ausência de e-mail ou de senha do administrador SHALL fazer a inicialização falhar com mensagem que identifique a variável ausente. A aplicação SHALL NOT subir sem administrador quando o bootstrap foi pedido.

#### Scenario: Missing password is reported by name

- **WHEN** a aplicação sobe com o bootstrap habilitado e sem senha do administrador
- **THEN** a inicialização falha
- **AND** a mensagem identifica a variável da senha do administrador

#### Scenario: Missing email is reported by name

- **WHEN** a aplicação sobe com o bootstrap habilitado e sem e-mail do administrador
- **THEN** a inicialização falha
- **AND** a mensagem identifica a variável do e-mail do administrador

### Requirement: Bootstrap password follows the user password policy

A senha do administrador inicial SHALL obedecer à mesma regra de senha dos demais usuários. Uma senha fora da regra SHALL fazer a inicialização falhar, sem criar o usuário.

#### Scenario: Short bootstrap password blocks startup

- **WHEN** a aplicação sobe com o bootstrap habilitado e senha do administrador com menos de 6 caracteres
- **THEN** a inicialização falha identificando a regra de senha violada
- **AND** nenhum usuário é criado

### Requirement: Bootstrap never alters an existing user

Se já existir usuário com o e-mail configurado, o bootstrap SHALL NOT criar, alterar ou redefinir a senha desse usuário.

#### Scenario: Existing admin keeps the current password

- **WHEN** a aplicação sobe com o bootstrap habilitado e já existe usuário com o e-mail configurado, com senha diferente da configurada
- **THEN** o usuário existente permanece inalterado
- **AND** a autenticação continua funcionando apenas com a senha atual dele

#### Scenario: Repeated startups create no duplicate

- **WHEN** a aplicação é reiniciada várias vezes com o bootstrap habilitado
- **THEN** existe exatamente um usuário com o e-mail configurado
