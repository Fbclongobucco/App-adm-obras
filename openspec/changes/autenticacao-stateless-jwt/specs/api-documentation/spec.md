## MODIFIED Requirements

### Requirement: Requests sent from the interface follow existing authorization

A descrição SHALL declarar o esquema de autenticação Bearer (access token) aceito pela API, e a interface SHALL permitir informar um access token. Requisições enviadas pela interface SHALL estar sujeitas às mesmas regras de autenticação e autorização de qualquer outro cliente.

#### Scenario: Request without credentials is refused

- **WHEN** uma rota protegida é executada pela interface sem access token
- **THEN** a API responde com recusa de autenticação, como responderia a qualquer cliente

#### Scenario: Request with insufficient role is refused

- **WHEN** um usuário com perfil `OPERADOR` informa seu access token na interface e executa uma operação de escrita restrita a `ADMIN`
- **THEN** a API responde com recusa de autorização

#### Scenario: Request with valid credentials succeeds

- **WHEN** um usuário `ADMIN` informa na interface o access token obtido no login e executa uma rota de leitura
- **THEN** a API responde com sucesso

#### Scenario: Description declares Bearer, not Basic

- **WHEN** a descrição da API é inspecionada
- **THEN** ela declara um esquema HTTP `bearer` e não declara o esquema `basic`
