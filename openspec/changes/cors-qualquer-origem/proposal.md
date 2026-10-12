## Why

O usuário quer que a API aceite chamadas de qualquer origem, para integrações e para o front em domínios variados. Hoje o CORS só aceita a lista explícita de `APP_CORS_ALLOWED_ORIGINS`, com credenciais.

## What Changes

- `/api/**`: qualquer origem, **sem credenciais** (cookies não são enviados nem aceitos); as rotas usam o token Bearer no cabeçalho.
- `/api/auth/**` (login, refresh, logout): continua só com as origens de `APP_CORS_ALLOWED_ORIGINS` e com credenciais, porque o refresh token vive em cookie. O `RefreshOriginFilter` não muda.
- Decisão do usuário entre três opções; a liberação total (com cookies em refresh) foi descartada por permitir que outro site renovasse a sessão.

## Capabilities

### New Capabilities

- `api-cors`: política de CORS da API.

### Modified Capabilities

<!-- Nenhuma. -->

## Impact

- `SecurityConfig.corsConfigurationSource`, testes de CORS, README e `.env.example`.
