## Why

O front chama refresh e logout pelo servidor do Next e precisava informar um `Origin` igual a uma entrada de `APP_CORS_ALLOWED_ORIGINS`. Ao publicar o front em outro endereço, bastava um dos dois `.env` divergir para a sessão cair com 403. O usuário pediu que o deploy do front não dependa desse ajuste.

## What Changes

- `RefreshOriginFilter`: refresh e logout **sem** cabeçalho `Origin` passam; com `Origin` fora da lista continuam 403.
- Motivo de segurança: o ataque que o filtro evita (CSRF com o cookie do refresh) só parte de navegador, e navegadores sempre enviam `Origin` em POST entre sites. Sem `Origin`, o cliente não é um navegador e não tem o cookie de outra pessoa.
- O front deixa de enviar `Origin` (remove `APP_ORIGIN`).

## Capabilities

### New Capabilities

- `refresh-origin`: verificação de origem em refresh e logout.

### Modified Capabilities

<!-- Nenhuma em openspec/specs. -->

## Impact

- `RefreshOriginFilter`, testes, README. Sem migração.
