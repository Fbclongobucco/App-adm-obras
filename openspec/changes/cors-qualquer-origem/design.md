## Context

`CorsProperties` recusa `*` e é usado também pelo `RefreshOriginFilter`, que protege refresh e logout (autenticados por cookie).

## Goals / Non-Goals

**Goals:** qualquer origem nas rotas com Bearer. **Non-Goals:** liberar cookies entre sites.

## Decisions

- Duas configurações no `UrlBasedCorsConfigurationSource`, registradas nesta ordem (vale a primeira que casa): `/api/auth/**` com as origens da lista e `allowCredentials=true`; `/api/**` com `allowedOriginPatterns=*` e `allowCredentials=false`.
- `CorsProperties` continua sem aceitar `*`: a lista segue valendo para autenticação e para o filtro de origem.

## Risks / Trade-offs

- [Qualquer site lê respostas da API] → só com um token Bearer válido, que o site não tem; sem cookies, o navegador não envia credenciais automaticamente.
