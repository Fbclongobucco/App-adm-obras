## Context

`AuthUseCase.refresh` hoje revoga a família em dois pontos: token com `revokeReason = ROTATED` reapresentado, e `markRotatedIfActive` retornando `false` (outra requisição trocou o mesmo token antes). A troca grava `revoked_at` (instante da troca) e `revoke_reason = ROTATED`. `revokeFamily` só altera tokens com `revoked_at` nulo, então tokens já trocados mantêm `ROTATED` e os ativos recebem `LOGOUT`, `REUSE` ou `USER_CHANGED`. Todos os tokens da família compartilham `expires_at`.

## Goals / Non-Goals

**Goals:**
- Aceitar reuso de token trocado há menos que a janela, sem estado novo no banco.
- Manter o comportamento atual quando a janela é `0`.

**Non-Goals:**
- Devolver o mesmo token B à requisição que perdeu a disputa (exigiria guardar o token em claro ou cifrado).
- Mudar prazos de access token ou de sessão, ou o contrato HTTP.
- Alterar o front.

## Decisions

1. **Emitir um token irmão, não repetir o sucessor.** Dentro da janela, o use case cria outro sucessor da mesma família (`current.successor(...)`), com a mesma expiração. Quem perdeu a disputa recebe C; quem ganhou fica com B; os dois seguem válidos até serem trocados. Cada cliente acaba usando o último cookie que recebeu, e o outro token expira no prazo da sessão ou é revogado junto com ela.
   - Alternativa: guardar o token B cifrado e devolvê-lo ao reuso. Descartada: mais estado sensível no banco, migração e chave adicional, sem ganho prático.
   - Alternativa: só no front (lock compartilhado, ex. Redis). Descartada: não cobre várias abas nem exige menos infraestrutura.

2. **Critério de "sessão aberta": nenhuma revogação da família com motivo diferente de `ROTATED`, e `expires_at` no futuro.** Nova consulta no repositório, ex. `isFamilyClosed(familyId)` (`exists` com `revoke_reason <> ROTATED`).
   - Alternativa: "existe token ativo na família". Descartada: entre `markRotatedIfActive` e o `save` do sucessor da outra requisição, a família fica sem token ativo e o critério recusaria um reuso legítimo.

3. **Instante da troca = `revoked_at` do token apresentado.** Dentro da janela se `now - revoked_at <= grace`. Na disputa (`markRotatedIfActive` falso), o use case relê o token pelo hash: se estiver `ROTATED`, aplica a mesma regra; se tiver outro motivo, recusa sem revogar de novo.

4. **Ordem das verificações em `refresh`:** hash inexistente → 401; expirado → 401; token `ROTATED` → janela (dentro e sessão aberta → irmão; senão → revoga família `REUSE`, 401); outro motivo de revogação → 401; usuário inativo → revoga `USER_CHANGED`, 401; troca normal. A verificação de usuário também vale para o caminho da janela.

5. **Propriedade em `JwtProperties`:** `refreshReuseGrace` (`Duration`, padrão 30 s). Validação no construtor: não negativa e menor que `accessTokenTtl` (janela maior que o access token deixaria o reuso de um token antigo passar após um ciclo completo). Injetada em `AuthUseCase` por `UseCaseConfig`.

## Risks / Trade-offs

- [Token roubado reapresentado dentro da janela gera sessão para o atacante] → janela curta (30 s), cookie `HttpOnly`/`Secure`, refresh só entre servidores no fluxo do front. Fora da janela a detecção de reuso segue igual.
- [Mais tokens ativos por família durante disputas] → limitado ao número de requisições simultâneas; todos têm a expiração da sessão e são apagados por `RefreshTokenCleanup`.
- [Relógio: `revoked_at` gravado pelo mesmo `Clock` do use case] → sem dependência de relógio do banco.

## Migration Plan

Sem migração de banco. Deploy da nova imagem do backend; a janela entra em vigor com o padrão de 30 s. Rollback: `APP_SECURITY_JWT_REFRESH_REUSE_GRACE=0` ou voltar a imagem anterior.
