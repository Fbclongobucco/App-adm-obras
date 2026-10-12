## ADDED Requirements

### Requirement: Origem em refresh e logout
Refresh e logout SHALL aceitar requisições sem cabeçalho `Origin` e com `Origin` presente em `APP_CORS_ALLOWED_ORIGINS`, e MUST responder 403 `auth.origin.forbidden` a `Origin` fora da lista, sem consumir nem revogar o token.

#### Scenario: Sem Origin
- **WHEN** o servidor do front chama `POST /api/auth/refresh` com o cookie e sem `Origin`
- **THEN** a API renova o token

#### Scenario: Origem estranha
- **WHEN** a chamada vem com `Origin: https://site-malicioso.example.com`
- **THEN** a API responde 403 e o token continua válido
