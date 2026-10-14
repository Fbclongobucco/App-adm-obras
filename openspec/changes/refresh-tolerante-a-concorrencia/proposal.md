## Why

Em produção o usuário precisa refazer o login várias vezes por dia. O refresh token troca a cada uso, e reapresentar um token já trocado revoga a sessão inteira. O front (Next no Vercel) dispara refreshes simultâneos com o mesmo token: proxy e página em instâncias diferentes, várias abas, prefetch de links. O primeiro troca o token; os demais caem na regra de reuso e derrubam a sessão a cada expiração do access token (15 min). O front já reduziu um dos casos (commit 5c2ce9e: proxy repassa o cookie renovado), mas não controla requisições paralelas em instâncias distintas. A tolerância precisa estar no backend.

## What Changes

- Janela de tolerância ao reuso: um refresh token já trocado, reapresentado dentro de poucos segundos após a troca, recebe um novo par de tokens da mesma sessão em vez de revogá-la.
- Reuso fora da janela continua revogando a sessão inteira, como hoje.
- A disputa entre duas requisições com o mesmo token ainda ativo (`markRotatedIfActive` falhou) segue a mesma regra da janela, sem revogar.
- Nova propriedade `app.security.jwt.refresh-reuse-grace` (padrão 30 s; `0` desliga a tolerância e volta ao comportamento atual).
- Sessão encerrada (logout, reuso fora da janela, usuário inativo) nunca é reaberta pela janela.
- Inalterado: access token de 15 min, prazo absoluto de 7 dias desde o login (decisão do usuário), rotação a cada uso, armazenamento só do hash, contrato HTTP de `/api/auth/refresh`.

## Capabilities

### New Capabilities

- `refresh-token-rotation`: regras de troca e reuso do refresh token, incluindo a janela de tolerância a refreshes concorrentes.

### Modified Capabilities

<!-- Nenhuma em openspec/specs. A regra "Reuse of a refresh token revokes its chain" está na change não arquivada autenticacao-stateless-jwt (spec token-authentication) e passa a ter a exceção da janela. Ajustar aquele texto no archive. -->

## Impact

- `AuthUseCase.refresh`, `RefreshTokenRepository` (consulta de sessão ativa), `JwtProperties`, configuração do use case, `application.properties`, README.
- Testes: `AuthUseCaseTest`, `RefreshTokenRepositoryAdapterTest`, `AuthControllerIntegrationTest`, `JwtProperties`.
- Sem migração: `revoked_at` e `revoke_reason` já registram quando e por que o token foi trocado.
- Front: nenhuma mudança obrigatória.
- Segurança: dentro da janela, quem tiver um token roubado e recém-trocado também obtém sessão. Risco aceito em troca da estabilidade; mitigado pela janela curta e pelo cookie `HttpOnly`.
