## Purpose

Autenticar clientes da API HTTP sem estado no servidor, por um access token assinado de vida curta, e manter o acesso por um refresh token rotativo e revogável, sem que o cliente precise guardar ou reenviar a senha.

## ADDED Requirements

### Requirement: Login issues an access token and a refresh token

Com e-mail e senha válidos de um usuário ativo, `POST /api/auth/login` SHALL responder com sucesso, trazer no corpo o access token, o tipo `Bearer`, a validade em segundos e o perfil do usuário (id, nome, e-mail, perfis de acesso), e entregar o refresh token num cookie. A resposta SHALL NOT conter senha nem hash de senha.

#### Scenario: Successful login returns both tokens

- **WHEN** um usuário ativo envia e-mail e senha corretos para o login
- **THEN** o corpo traz access token, tipo `Bearer`, validade em segundos e o perfil do usuário, e a resposta define o cookie do refresh token

#### Scenario: Login response never exposes the password

- **WHEN** o login é bem-sucedido
- **THEN** nenhum campo da resposta contém a senha ou o hash da senha

### Requirement: Refresh token travels only in a protected cookie

O refresh token SHALL ser entregue e recebido só por cookie inacessível a scripts, restrito ao host que o emitiu e aos caminhos `/api/auth`, com validade igual à do token e marcado como seguro (só HTTPS) fora do desenvolvimento local. O valor SHALL NOT aparecer em corpo de resposta.

#### Scenario: Cookie attributes

- **WHEN** o login é bem-sucedido no perfil de produção com a configuração padrão
- **THEN** o cookie do refresh token é `HttpOnly`, `Secure`, `SameSite=Strict`, tem `Path=/api/auth`, não tem `Domain` e expira junto com o refresh token

#### Scenario: Refresh token is absent from the body

- **WHEN** o corpo da resposta do login ou do refresh é inspecionado
- **THEN** ele não contém o valor do refresh token

#### Scenario: Cookie is not sent to business routes

- **WHEN** o navegador chama uma rota fora de `/api/auth`
- **THEN** o cookie do refresh token não é enviado

### Requirement: Cookie policy is configurable per deployment topology

A política `SameSite` do cookie SHALL ser configurável por ambiente, com `Strict` como padrão, para suportar frontend e API no mesmo site ou em sites diferentes. `SameSite=None` SHALL exigir `Secure`. O cookie SHALL poder ser marcado opcionalmente como `Partitioned`.

#### Scenario: Default is Strict

- **WHEN** nenhuma política é configurada
- **THEN** o cookie do refresh token sai com `SameSite=Strict`

#### Scenario: Cross-site deployment

- **WHEN** a política configurada é `None`, com `Secure` ativo
- **THEN** o cookie do refresh token sai com `SameSite=None` e `Secure`

#### Scenario: Partitioned cookie on request

- **WHEN** a opção de cookie particionado está ativa
- **THEN** o cookie do refresh token sai com o atributo `Partitioned`, e sem ela o atributo não aparece

#### Scenario: Removal cookie keeps the same attributes

- **WHEN** o logout ou um refresh recusado apaga o cookie
- **THEN** o cookie de remoção usa o mesmo nome, `Path`, `SameSite`, `Secure` e `Partitioned` do cookie emitido, com expiração imediata

### Requirement: Invalid cookie configuration stops startup

A aplicação SHALL falhar ao iniciar, com mensagem que nomeia a variável de configuração envolvida, quando a política `SameSite` for desconhecida, quando for `None` sem `Secure`, quando `Secure` estiver desligado no perfil de produção, ou quando a lista de origens permitidas do frontend contiver curinga.

#### Scenario: None without Secure

- **WHEN** a aplicação inicia com `SameSite=None` e `Secure` desligado
- **THEN** a inicialização falha e a mensagem nomeia as duas variáveis

#### Scenario: Unknown policy

- **WHEN** a aplicação inicia com uma política `SameSite` fora de `Strict`, `Lax` e `None`
- **THEN** a inicialização falha e a mensagem nomeia a variável

#### Scenario: Insecure cookie in production

- **WHEN** a aplicação inicia no perfil de produção com `Secure` desligado
- **THEN** a inicialização falha

#### Scenario: Wildcard origin

- **WHEN** a lista de origens permitidas do frontend contém `*`
- **THEN** a inicialização falha, porque cookie com credenciais exige origens explícitas

### Requirement: Login rejects invalid credentials uniformly

O login SHALL recusar com 401 a senha incorreta, o e-mail inexistente, o usuário inativo e a requisição sem e-mail ou senha. A resposta SHALL ser a mesma nos quatro casos, sem revelar se o e-mail existe, e SHALL NOT emitir token.

#### Scenario: Wrong password

- **WHEN** um e-mail existente é enviado com senha incorreta
- **THEN** a resposta é 401 e nenhum token é emitido

#### Scenario: Unknown email is indistinguishable from wrong password

- **WHEN** um e-mail inexistente é enviado
- **THEN** a resposta é 401, com o mesmo código e mensagem do caso de senha incorreta, e a mensagem não contém o e-mail enviado

#### Scenario: Inactive user cannot log in

- **WHEN** um usuário inativo envia e-mail e senha corretos
- **THEN** a resposta é 401 e nenhum token é emitido

#### Scenario: Missing credentials

- **WHEN** o login é chamado sem corpo, sem e-mail ou sem senha
- **THEN** a resposta é 401 e nenhum token é emitido

### Requirement: Protected routes require a valid access token

Toda rota que hoje exige autenticação SHALL aceitar o cliente somente com um access token válido no cabeçalho `Authorization: Bearer`. Token ausente, malformado, com assinatura inválida, expirado ou de outro emissor SHALL resultar em 401.

#### Scenario: Valid token grants access

- **WHEN** um cliente chama uma rota de leitura com um access token válido de um usuário `OPERADOR`
- **THEN** a resposta é bem-sucedida

#### Scenario: Missing token is refused

- **WHEN** um cliente chama uma rota protegida sem cabeçalho `Authorization`
- **THEN** a resposta é 401

#### Scenario: Tampered token is refused

- **WHEN** um cliente envia um access token com conteúdo ou assinatura alterados
- **THEN** a resposta é 401

#### Scenario: Expired token is refused

- **WHEN** um cliente envia um access token cuja validade já terminou
- **THEN** a resposta é 401

### Requirement: HTTP Basic is no longer accepted

A API SHALL NOT autenticar por HTTP Basic. Credenciais Basic, mesmo corretas, SHALL ser tratadas como ausência de autenticação.

#### Scenario: Correct Basic credentials are refused

- **WHEN** um cliente chama uma rota protegida com `Authorization: Basic` contendo e-mail e senha corretos
- **THEN** a resposta é 401

### Requirement: Authorization rules are unchanged

Os perfis de acesso do usuário SHALL ser obtidos do access token, e as regras de autorização SHALL continuar as atuais: leitura sob `/api` para `ADMIN` e `OPERADOR`, escrita sob `/api` só para `ADMIN`.

#### Scenario: Operator cannot write

- **WHEN** um usuário `OPERADOR` com access token válido chama uma operação de escrita sob `/api`
- **THEN** a resposta é 403

#### Scenario: Admin can write

- **WHEN** um usuário `ADMIN` com access token válido chama uma operação de escrita sob `/api` com dados válidos
- **THEN** a operação é aceita

### Requirement: The server keeps no authentication session

O servidor SHALL NOT criar nem consultar sessão HTTP para autenticar. Cada requisição SHALL ser autenticada só pelo token que ela traz.

#### Scenario: No session cookie is issued

- **WHEN** um cliente faz login e depois chama rotas protegidas com o access token
- **THEN** nenhuma resposta define cookie de sessão

#### Scenario: Previous request does not authenticate the next one

- **WHEN** um cliente faz uma requisição autenticada e, em seguida, outra sem token
- **THEN** a segunda resposta é 401

### Requirement: Access token has a short, configured lifetime

O access token SHALL expirar após uma validade configurável, informada na resposta do login e do refresh. Ele SHALL identificar o usuário e seus perfis de acesso e SHALL NOT conter senha, hash ou outro segredo.

#### Scenario: Reported lifetime matches the token

- **WHEN** um access token é emitido
- **THEN** a validade informada na resposta corresponde à expiração gravada no token

#### Scenario: Token carries no secret

- **WHEN** o conteúdo de um access token é decodificado
- **THEN** ele identifica o usuário e seus perfis, e não contém senha nem hash

### Requirement: Current user profile is resolved from the token

`GET /api/auth/me` SHALL devolver o perfil do usuário identificado pelo access token, com os dados atuais do cadastro.

#### Scenario: Profile of the token owner

- **WHEN** um usuário chama `GET /api/auth/me` com access token válido
- **THEN** a resposta traz id, nome, e-mail e perfis de acesso desse usuário

#### Scenario: Profile requires a token

- **WHEN** `GET /api/auth/me` é chamado sem access token
- **THEN** a resposta é 401

### Requirement: Refresh exchanges a refresh token for a new token pair

`POST /api/auth/refresh` SHALL ler o refresh token do cookie, sem exigir access token, e responder com um novo access token no corpo, no mesmo formato do login, e um novo refresh token no cookie.

#### Scenario: Valid refresh token

- **WHEN** um cliente chama o refresh com o cookie de um refresh token válido e não usado
- **THEN** a resposta é bem-sucedida, traz um novo access token e substitui o cookie por um novo refresh token

#### Scenario: Missing cookie

- **WHEN** o refresh é chamado sem o cookie do refresh token
- **THEN** a resposta é 401 e nenhum token é emitido

#### Scenario: Refresh works after the access token expired

- **WHEN** o access token do cliente já expirou e ele envia um refresh token válido
- **THEN** o refresh é bem-sucedido

### Requirement: Refresh tokens are single-use

Cada refresh token SHALL ser aceito uma única vez. Ao ser trocado, ele SHALL deixar de valer.

#### Scenario: Used refresh token is refused

- **WHEN** um refresh token já trocado é enviado de novo
- **THEN** a resposta é 401 e nenhum token é emitido

### Requirement: Reuse of a refresh token revokes its chain

Se um refresh token já trocado for reapresentado, o sistema SHALL revogar todos os refresh tokens que descendem do mesmo login, inclusive o mais recente.

#### Scenario: Replay revokes the current token too

- **WHEN** o refresh token A é trocado por B, e depois A é reapresentado
- **THEN** a reapresentação de A é recusada com 401, e B passa a ser recusado com 401

#### Scenario: Other logins are not affected

- **WHEN** a cadeia de um login é revogada por reapresentação
- **THEN** os refresh tokens de outro login do mesmo usuário continuam válidos

### Requirement: Refresh token is rejected when invalid

O refresh SHALL recusar com 401, sem emitir token, um refresh token inexistente, expirado, revogado ou pertencente a usuário inativo. A resposta SHALL ser a mesma em todos esses casos e SHALL apagar o cookie do refresh token.

#### Scenario: Unknown refresh token

- **WHEN** um valor que nunca foi emitido é enviado como refresh token
- **THEN** a resposta é 401

#### Scenario: Expired refresh token

- **WHEN** um refresh token cuja validade terminou é enviado
- **THEN** a resposta é 401

#### Scenario: Owner was deactivated

- **WHEN** o usuário dono de um refresh token não usado foi desativado
- **THEN** o refresh é recusado com 401

### Requirement: Refreshed access token reflects current user data

O access token emitido no refresh SHALL refletir os perfis de acesso atuais do usuário, e não os do login original.

#### Scenario: Role change takes effect on refresh

- **WHEN** um usuário deixa de ser `ADMIN` e depois faz refresh
- **THEN** o novo access token não concede o perfil `ADMIN`

### Requirement: Refresh tokens have an absolute, configured lifetime

A validade do refresh token SHALL ser configurável, maior que a do access token, e contada a partir do login. Os refresh tokens emitidos por rotação SHALL herdar a expiração do login original, sem estendê-la.

#### Scenario: Refresh token expires

- **WHEN** um refresh token é usado depois de terminada sua validade
- **THEN** o refresh é recusado com 401

#### Scenario: Rotation does not extend the session

- **WHEN** um cliente faz refresh repetidamente desde o login
- **THEN** todo refresh token emitido expira no mesmo instante que o primeiro, e após esse instante um novo login é necessário

### Requirement: Refresh tokens are stored only as a hash

O sistema SHALL guardar apenas um hash de cada refresh token. O valor do token SHALL ser aleatório, imprevisível e não derivado de dados do usuário, e SHALL NOT ser gravado em log.

#### Scenario: Stored data cannot be used as a token

- **WHEN** o valor gravado no armazenamento de um refresh token é enviado ao refresh
- **THEN** a resposta é 401

#### Scenario: Tokens are unique

- **WHEN** vários logins do mesmo usuário são feitos em sequência
- **THEN** cada um recebe um refresh token diferente

### Requirement: Logout revokes the refresh token

`POST /api/auth/logout` SHALL revogar o refresh token do cookie e apagar o cookie. O logout SHALL responder com sucesso e sem corpo mesmo sem cookie ou com token desconhecido, expirado ou revogado, sem revelar o estado do token.

#### Scenario: Refresh after logout fails

- **WHEN** um cliente faz logout e depois chama o refresh com o valor do refresh token que tinha
- **THEN** o logout é bem-sucedido, apaga o cookie, e o refresh é recusado com 401

#### Scenario: Logout with unknown token

- **WHEN** o logout é chamado sem cookie ou com um valor que não é refresh token válido
- **THEN** a resposta é bem-sucedida, sem corpo

#### Scenario: Access token remains valid until expiry

- **WHEN** um cliente faz logout e usa o access token antes de ele expirar
- **THEN** a requisição é aceita, porque o access token não é revogado

### Requirement: Cookie-authenticated endpoints refuse other origins

Como o navegador envia o cookie automaticamente, `POST /api/auth/refresh` e `POST /api/auth/logout` SHALL recusar com 403, sem efeito, requisições vindas de origem que não esteja entre as origens permitidas do frontend.

#### Scenario: Forged request from another site

- **WHEN** uma página de origem não permitida dispara o refresh ou o logout no navegador de um usuário logado
- **THEN** a resposta é 403, nenhum token é emitido ou revogado e o cookie não muda

#### Scenario: Frontend origin is accepted

- **WHEN** o frontend, numa origem permitida, chama o refresh com o cookie válido
- **THEN** o refresh é bem-sucedido

### Requirement: Deactivation and password change revoke refresh tokens

Ao desativar um usuário ou trocar a senha dele, o sistema SHALL revogar todos os refresh tokens desse usuário.

#### Scenario: Password change ends existing sessions

- **WHEN** a senha de um usuário é alterada
- **THEN** todo refresh token emitido antes da alteração é recusado com 401

#### Scenario: Deactivation ends existing sessions

- **WHEN** um usuário é desativado
- **THEN** todo refresh token dele é recusado com 401

#### Scenario: Other updates keep sessions

- **WHEN** só o nome de um usuário é alterado
- **THEN** os refresh tokens dele continuam válidos

### Requirement: Production requires an explicit signing key

No perfil de produção, a chave de assinatura dos tokens SHALL vir de configuração externa, sem valor padrão. Sem ela, ou com chave fora do tamanho mínimo do algoritmo, a aplicação SHALL falhar ao iniciar, com mensagem que nomeia a variável.

#### Scenario: Missing key stops startup

- **WHEN** a aplicação inicia no perfil de produção sem a chave de assinatura configurada
- **THEN** a inicialização falha e a mensagem nomeia a variável ausente

#### Scenario: Development key is not usable in production

- **WHEN** a configuração de produção é inspecionada
- **THEN** ela não contém chave de assinatura nem valor padrão para ela
