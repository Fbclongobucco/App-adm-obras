## ADDED Requirements

### Requirement: CORS das rotas de negócio
As rotas `/api/**` fora de `/api/auth/**` SHALL aceitar qualquer origem, sem credenciais.

#### Scenario: Origem qualquer
- **WHEN** uma requisição de pré-voo vem de `https://qualquer.exemplo` para `/api/projects`
- **THEN** a resposta permite essa origem e não envia `Access-Control-Allow-Credentials: true`

### Requirement: CORS da autenticação
As rotas `/api/auth/**` SHALL aceitar só as origens de `APP_CORS_ALLOWED_ORIGINS`, com credenciais.

#### Scenario: Origem não listada no refresh
- **WHEN** uma requisição de pré-voo vem de `https://qualquer.exemplo` para `/api/auth/refresh`
- **THEN** a origem não é permitida
