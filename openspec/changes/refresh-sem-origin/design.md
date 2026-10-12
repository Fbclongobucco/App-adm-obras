## Context

`RefreshOriginFilter` exigia `Origin` presente e listado. O cookie do refresh tem `SameSite=Strict` por padrão, e o front Next guarda o cookie no servidor, não no navegador.

## Goals / Non-Goals

**Goals:** deploy do front sem casar URL com o backend. **Non-Goals:** mudar o CORS ou o cookie.

## Decisions

- Ausente → passa; presente e listado → passa; presente e fora da lista → 403 `auth.origin.forbidden`.
- **Alternativa:** derivar a origem do front automaticamente. Descartada porque ainda exigiria listar o endereço no backend.

## Risks / Trade-offs

- [Navegador antigo que omite `Origin` em POST de formulário entre sites] → o cookie é `SameSite=Strict` por padrão e não é enviado entre sites; com `None` (deploy em sites distintos sem proxy), o README orienta manter o front no mesmo site ou usar proxy.
