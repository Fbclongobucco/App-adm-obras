## Context

Motivação e decisões do usuário em `proposal.md`. Requisitos em `specs/token-authentication/spec.md` e `specs/api-documentation/spec.md`.

Estado atual relevante:

- **Spring Boot 4.1.1, Java 25, Spring MVC, Spring Security 7.1.1.** O parent gerencia `spring-boot-starter-security-oauth2-resource-server`, que traz `spring-security-oauth2-jose` (Nimbus) com `NimbusJwtEncoder` e `NimbusJwtDecoder`.
- **`SecurityConfig`.** Um `SecurityFilterChain` com `csrf.disable()`, `httpBasic`, sem política de sessão declarada. A resposta autenticada já devolveu `JSESSIONID`. As regras de rota são: login, `/error`, springdoc e `/h2-console/**` liberados; `GET /api/**` para `ADMIN`/`OPERADOR`; o resto de `/api/**` só para `ADMIN`.
- **Fluxo de autenticação.** `AuthController` → `AuthUseCase` → porta `core/security/AuthenticationGateway` → `DatabaseAuthenticationGateway` (BCrypt via `PasswordHasher`). Nas outras requisições, o Basic passa por `AppUserDetailsService`.
- **Vazamento no erro de login.** `AuthenticationFailedException.unknownEmail` devolve o e-mail na mensagem. Isso viola o requisito de resposta uniforme.
- **Camadas.** `core/` e `application/` não importam framework. Use cases são montados em `infra/config/UseCaseConfig`.
- **Usuário.** `User` tem `id` (UUID), `email` (pode mudar por `PUT /api/users/{id}`), `isActive`, `passwordHash` e `roles`. `UserUseCase` faz create/update/delete.
- **Schema.** Hoje é `ddl-auto` (`update` em `local`, `create-drop` em `test`, `validate` em `prod`). O change `deploy-viavel`, não aplicado, introduz Flyway com `V1` baseline.
- **CORS.** Liberado para `app.cors.allowed-origins` em `/api/**`, com `allowCredentials=true`.
- **Testes.** `AuthControllerIntegrationTest` e `OpenApiDocumentationIntegrationTest` usam Basic. `SecurityConfigTest` usa `@WithMockUser` e não depende do mecanismo.

## Goals / Non-Goals

**Goals:**
- Manter as portas de `core/` livres de JWT, Nimbus e Spring. O formato do token é detalhe de `infra/`.
- Garantir uso único do refresh token mesmo com requisições concorrentes.
- Configuração de produção que falha cedo, sem segredo padrão.

**Non-Goals:**
- Lista de bloqueio (deny-list) de access tokens.
- Janela de tolerância para refresh concorrente legítimo, por exemplo duas abas.
- Rotação da chave de assinatura sem derrubar os access tokens emitidos (`kid` com várias chaves).

## Decisions

### D1. Resource server do Spring Security com Nimbus, HS256

**Escolha.** `spring-boot-starter-security-oauth2-resource-server`. `.oauth2ResourceServer(o -> o.jwt(...))` valida o token, e `NimbusJwtEncoder`/`NimbusJwtDecoder` usam a mesma `SecretKeySpec` HmacSHA256. O decoder valida assinatura, `exp` e `iss` (`JwtValidators.createDefaultWithIssuer`).

**Segredo.** Fica em Base64, em `app.security.jwt.secret`, e precisa ter no mínimo 32 bytes depois de decodificado. O tamanho é verificado no boot, com mensagem que nomeia `APP_JWT_SECRET`.

**Alternativas descartadas.**
- *Biblioteca jjwt com filtro próprio.* Repete o que o resource server já faz (extração do Bearer, entry point 401 com `WWW-Authenticate`, conversão de authorities), com mais código para manter.
- *RS256.* Descartado pelo usuário. Só esta API emite e valida o token, então a chave pública não teria consumidor.

### D2. Conteúdo do access token

- `iss`: `app-adm-obras` (configurável).
- `sub`: o `id` (UUID) do usuário. O e-mail não serve, porque pode mudar e quebraria `/me` e a revogação.
- `roles`: lista com os nomes dos perfis (`["ADMIN"]`).
- `iat` e `exp`, com `exp = iat + 15 min`.
- Sem nome, e-mail, senha ou hash. O perfil completo vem de `/me`.

`JwtAuthenticationConverter` com `authoritiesClaimName("roles")` e `authorityPrefix("ROLE_")` mantém o `hasRole` atual. `principalClaimName` fica no `sub`, e `/me` passa a buscar o usuário por id.

### D3. Portas no core, regras na aplicação, formato na infra

Novos elementos em `core/`, sem framework:
- `core/domain/RefreshToken`: `id`, `userId`, `tokenHash`, `familyId`, `expiresAt`, `createdAt`, `revokedAt`, `revokeReason` (`ROTATED`, `LOGOUT`, `REUSE`, `USER_CHANGED`). Inclui as regras `isActive(now)` e `rotate(...)`.
- `core/repository/RefreshTokenRepository`: `save`, `findByHash`, `markRotatedIfActive(id, now)` (retorna `boolean`), `revokeFamily(familyId, reason, now)`, `revokeAllByUser(userId, reason, now)`, `deleteExpiredBefore(instant)`.
- `core/security/AccessTokenIssuer`: `IssuedAccessToken issue(User user)`, um record com o valor e o `expiresAt`.

Em `application/`:
- `RefreshTokenFactory`: gera 32 bytes com `SecureRandom`, codifica em Base64URL sem padding e calcula o hash SHA-256 em hex. Usa só o JDK.
- `AuthUseCase`: `login`, `refresh(rawToken)`, `logout(rawToken)` e `profile(userId)`. Recebe `java.time.Clock` para os testes controlarem o tempo.
- `AuthResult`: record interno com o DTO de resposta, o refresh token em claro e sua expiração. Ele não é serializado. O controller usa o valor em claro só para montar o cookie.

Em `infra/`:
- `JwtAccessTokenIssuer`, `RefreshTokenEntity`, `RefreshTokenJpaRepository`, `RefreshTokenRepositoryAdapter`, `JwtProperties` e `RefreshCookieProperties`.

**Por que SHA-256 e não BCrypt no refresh token.** O token tem 256 bits aleatórios, sem dicionário para atacar, então o custo do BCrypt não acrescenta proteção. Além disso, a busca exige hash determinístico e indexável (`findByHash`).

### D4. Rotação atômica e detecção de reuso

Fluxo de `refresh(raw)` numa transação:
1. Calcular `hash(raw)` e buscar o token. Se não existir: 401.
2. Se o token estiver revogado com motivo `ROTATED`, é reuso: `revokeFamily(familyId, REUSE)` e 401. Com qualquer outro motivo de revogação, ou expirado: 401.
3. Se o usuário estiver inativo: `revokeFamily(..., USER_CHANGED)` e 401.
4. `markRotatedIfActive(id, now)`, via `UPDATE ... SET revoked_at = :now, revoke_reason = 'ROTATED' WHERE id = :id AND revoked_at IS NULL`. Se afetar 0 linhas, outra requisição já trocou o token; tratar como reuso (passo 2).
5. Gravar o novo token com o mesmo `familyId` e o mesmo `expiresAt` (validade absoluta) e emitir um novo access token com os perfis atuais.

O `UPDATE` condicional garante o uso único sem lock explícito. Duas requisições simultâneas com o mesmo token não conseguem ambas rotacionar.

`login` cria uma família nova (`familyId` aleatório), com `expiresAt = now + 7 dias`.

### D5. Tabela `refresh_token`

```
refresh_token
  id            UUID PK
  user_id       UUID NOT NULL  FK app_user(id) ON DELETE CASCADE
  token_hash    VARCHAR(64) NOT NULL UNIQUE
  family_id     UUID NOT NULL          -- índice
  expires_at    TIMESTAMP NOT NULL
  created_at    TIMESTAMP NOT NULL
  revoked_at    TIMESTAMP NULL
  revoke_reason VARCHAR(20) NULL
  índice em user_id
```

O `ON DELETE CASCADE` mantém `DELETE /api/users/{id}` funcionando. Na entidade, use `@OnDelete(action = OnDeleteAction.CASCADE)` para o `ddl-auto` gerar a mesma regra.

**Migração.** A estratégia depende de `deploy-viavel` no momento do apply. Com Flyway presente, criar a próxima versão `V<n>__refresh_token.sql`. Sem Flyway, a entidade basta para `local`/`test`, e `prod` com `validate` exige que a tabela seja criada à mão. Essa condição fica registrada no README. A task 2.4 verifica qual caso vale.

### D6. Cookie do refresh token

- Nome `refresh_token`, `HttpOnly`, `Path=/api/auth`, `Max-Age` igual ao tempo restante até `expiresAt`.
- Sem atributo `Domain`: o cookie fica preso ao host que o emitiu. O navegador só precisa devolvê-lo a esse host, e omitir `Domain` impede que subdomínios irmãos o recebam.
- `Secure`: `app.security.refresh-cookie.secure`, `true` por padrão e `false` em `local` e `test` (HTTP sem TLS). Em `prod`, fixo em `true` (D14).
- `SameSite`: `app.security.refresh-cookie.same-site`, `Strict` por padrão.
- `Partitioned`: `app.security.refresh-cookie.partitioned`, `false` por padrão.
- Em logout e em refresh recusado, o cookie é apagado com o mesmo nome, `Path`, `SameSite`, `Secure` e `Partitioned`, com `Max-Age=0`. Um cookie de remoção com atributos diferentes pode não substituir o original.
- Montado com `ResponseCookie`, só no `AuthController`. `application/` não conhece cookie.

O frontend chama `/api/auth/*` com `credentials: 'include'`, o que o CORS atual já permite (`allowCredentials=true`).

**Topologias suportadas.** "Site" é o domínio registrável (ex.: `empresa.com.br`); a porta e o subdomínio não contam.

| Topologia | Exemplo | Configuração | Cookie de terceiros bloqueado |
|---|---|---|---|
| A. Mesmo site | `app.empresa.com.br` → `api.empresa.com.br`; `localhost:3000` → `localhost:8080` | `same-site=Strict` (padrão) | Não afeta |
| B. Proxy same-origin no Next.js | navegador → `app.empresa.com.br/api/...` → API interna | `same-site=Strict` (padrão) | Não afeta |
| C. Sites diferentes, sem proxy | `app.foo.com.br` → `api.bar.com.br` | `same-site=None`, `secure=true`, `partitioned` opcional | Pode descartar o cookie |

- **A.** É o caso do desenvolvimento local. Nenhuma configuração extra.
- **B.** Recomendada quando os domínios diferem. Um `rewrites()` no `next.config` encaminha `/api/:path*` para a URL da API mantendo o prefixo `/api`, então `Path=/api/auth` continua valendo. Para o navegador, o cookie é do host do frontend, ou seja, primário. O `Origin` que chega à API é o do frontend e precisa estar em `app.cors.allowed-origins` (D7). O CORS deixa de ser exercido pelo navegador, mas a verificação de origem continua ativa.
- **C.** Funciona onde o navegador aceita cookie de terceiros. Navegadores com bloqueio de cookies de terceiros ativo (por padrão ou por escolha do usuário) podem não enviar o cookie: o refresh falha, e o usuário precisa logar de novo quando o access token expira. `Partitioned` (CHIPS) pode manter o cookie disponível em navegadores que o suportam, mas o suporte varia entre navegadores e versões; confirmar na implementação e não tratar como garantia.

### D7. Proteção de origem em refresh e logout, sem CSRF token

**Escolha.** Um filtro em `infra/security`, aplicado só a `POST /api/auth/refresh` e `POST /api/auth/logout`, exige o cabeçalho `Origin` presente e contido em `app.cors.allowed-origins`. Sem isso, a resposta é 403, antes do controller. O `csrf()` do Spring continua desabilitado: as demais rotas usam Bearer em cabeçalho, que o navegador não envia sozinho.

Navegadores enviam `Origin` em todo `POST` de origem cruzada e, nos navegadores atuais, também no de mesma origem. Clientes fora do navegador (curl, testes) precisam enviar `Origin` explicitamente para usar o refresh.

**Alternativas descartadas.**
- *CSRF token do Spring (`CookieCsrfTokenRepository`).* Exige que o frontend leia um cookie emitido pelo domínio da API. Quebra se frontend e API ficarem em sites diferentes em produção, e acrescenta um round-trip.
- *Só `SameSite=Strict`.* Protege enquanto frontend e API forem do mesmo site. A verificação de origem mantém a proteção se for preciso usar `SameSite=None`.

Com `SameSite=None` (topologia C), a verificação de origem é a proteção principal contra requisição forjada, porque o navegador passa a enviar o cookie em requisições de outros sites.

**Efeito colateral.** O Swagger UI (origem `:8080`) não chama o refresh. Lá o fluxo é login e colar o access token no "Authorize", o que basta para teste manual.

### D8. `SecurityConfig` stateless

- `sessionManagement(s -> s.sessionCreationPolicy(STATELESS))`.
- Remover `.httpBasic(...)`.
- `oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(...)))`.
- `permitAll` para `POST /api/auth/login`, `/api/auth/refresh` e `/api/auth/logout`. As regras atuais de `/api/**`, `/error`, springdoc e `/h2-console/**` não mudam, nem o `frameOptions(sameOrigin)`.
- Remover `AppUserDetailsService` e o teste dele. A verificação de senha do login continua em `DatabaseAuthenticationGateway`.
- Garantir que o Spring Boot não crie usuário em memória com senha gerada. Verificar no log do boot (task 3.1).

### D9. Resposta de login uniforme

`AuthenticationFailedException.unknownEmail` passa a usar a mesma mensagem de `invalidCredentials`, sem o e-mail. O código `auth.invalid.credentials` já é igual. O refresh recusado usa uma exceção própria (`auth.invalid.refresh`) que também resulta em 401 no `GlobalExceptionHandler`.

### D10. Revogação por mudança no usuário

`UserUseCase.update` compara o estado antes e depois da alteração. Se a senha foi enviada, ou se `isActive` passou de `true` para `false`, chama `refreshTokenRepository.revokeAllByUser(id, USER_CHANGED)`. Mudança de nome, e-mail ou perfis não revoga: os perfis novos já entram no próximo refresh (requisito "Refreshed access token reflects current user data").

### D11. Configuração e fail-fast

```
app.security.jwt.issuer=app-adm-obras
app.security.jwt.access-token-ttl=15m
app.security.jwt.refresh-token-ttl=7d
app.security.jwt.secret=           # local/test: segredo fixo de desenvolvimento
                                   # prod: ${APP_JWT_SECRET}, sem default
app.security.refresh-cookie.secure=${APP_REFRESH_COOKIE_SECURE:true}            # false em local/test; true fixo em prod
app.security.refresh-cookie.same-site=${APP_REFRESH_COOKIE_SAME_SITE:Strict}
app.security.refresh-cookie.partitioned=${APP_REFRESH_COOKIE_PARTITIONED:false}
```

`JwtProperties` é um record `@ConfigurationProperties` com `Duration`. Em `prod`, `${APP_JWT_SECRET}` sem default chega ao binder como o texto literal do placeholder quando a variável não existe (verificado na implementação). O construtor de `JwtProperties` trata esse caso como segredo ausente e falha com mensagem que nomeia `APP_JWT_SECRET`. Um segredo curto ou em Base64 inválido falha no construtor de `JwtProperties`, com mensagem que nomeia a variável. O segredo de `local`/`test` fica versionado, marcado como de desenvolvimento, e não vale em `prod` porque o arquivo de `prod` não o herda (o arquivo `application.properties` não define segredo).

### D14. Validação da configuração do cookie no boot

`RefreshCookieProperties` é um record `@ConfigurationProperties` que valida no construtor:
- `same-site` em `Strict`, `Lax` ou `None`, sem diferenciar maiúsculas. Outro valor falha nomeando `APP_REFRESH_COOKIE_SAME_SITE`.
- `same-site=None` com `secure=false` falha nomeando `APP_REFRESH_COOKIE_SAME_SITE` e `APP_REFRESH_COOKIE_SECURE`. Navegadores rejeitam `SameSite=None` sem `Secure`.

Em `application-prod.properties`, `app.security.refresh-cookie.secure=true` é literal, sem placeholder, para que nenhuma variável o desligue em produção. A regra "Secure desligado em prod falha" é coberta assim: o valor não é configurável no perfil. Um teste de recursos fixa isso.

`CorsProperties` passa a rejeitar `*` em `allowed-origins`, nomeando `APP_CORS_ALLOWED_ORIGINS`. O Spring já recusaria `*` com `allowCredentials=true`, mas só na primeira requisição. A validação antecipa o erro para o boot.

**Por que validar no boot.** Um `SameSite=None` sem `Secure` sobe sem erro, mas o navegador descarta o cookie em silêncio, e o defeito só aparece como "usuário deslogado a cada 15 min" em produção.

### D12. Limpeza de tokens expirados

`@Scheduled` diário em `infra/` chama `deleteExpiredBefore(now)`, apagando os tokens com `expires_at` passado. Requer `@EnableScheduling`. Sem isso, a tabela cresce sem limite. Expirados não têm uso, nem para detectar reuso, porque um token expirado já é recusado.

### D13. OpenAPI com Bearer

`OpenApiConfig` troca `basicAuth` por `bearerAuth` (`type: http`, `scheme: bearer`, `bearerFormat: JWT`), mantendo o requisito global.

## Risks / Trade-offs

- **[Access token continua válido por até 15 min após logout, desativação ou troca de perfil]** → Validade curta e revogação dos refresh tokens. Deny-list fica fora do escopo.
- **[Refresh concorrente legítimo derruba a sessão]** Duas abas renovando ao mesmo tempo com o mesmo cookie disparam a detecção de reuso (D4). → O frontend deve serializar o refresh (uma requisição em andamento, as outras aguardam). Fica registrado no README para o frontend.
- **[Segredo HS256 vazado permite forjar tokens de qualquer usuário]** → Segredo só em variável de ambiente, nunca no repositório para `prod`. A troca do segredo invalida todos os access tokens; os refresh tokens continuam válidos e geram tokens novos.
- **[`SameSite=Strict` impede o refresh se frontend e API estiverem em sites diferentes sem proxy]** → Topologia B (proxy) ou C (`SameSite=None`), conforme D6. O README traz as três configurações.
- **[Topologia C: navegador com bloqueio de cookie de terceiros descarta o refresh token]** → O usuário volta ao login a cada expiração do access token. Mitigação: preferir a topologia B; `Partitioned` como opção, sem garantia de suporte. O risco não tem solução só no backend.
- **[Configuração de cookie incoerente passa despercebida]** → Validação no boot (D14).
- **[Clientes fora do navegador precisam enviar `Origin` para refresh]** → Documentado. Integrações máquina a máquina não estão no escopo.
- **[BREAKING para o frontend]** → Deploy coordenado (Migration Plan).

## Migration Plan

1. Pré-requisito de arquivamento: arquivar `documentacao-api` antes deste change.
2. Gerar o segredo de produção, por exemplo com `openssl rand -base64 32`, e definir `APP_JWT_SECRET` no ambiente antes do deploy.
3. A tabela `refresh_token` é criada pela migração `V2__refresh_token.sql` no boot (Flyway instalado pelo grupo 2 do `deploy-viavel`).
4. Escolher a topologia (D6) e, se for C, definir `APP_REFRESH_COOKIE_SAME_SITE=None`. Se for B, configurar o `rewrites()` no Next.js e incluir a origem do frontend em `APP_CORS_ALLOWED_ORIGINS`.
5. Publicar backend e frontend juntos. Não há período de compatibilidade com Basic.
6. **Rollback.** Voltar à imagem anterior. A tabela `refresh_token` fica sem uso e não afeta a versão anterior. Os usuários fazem login de novo em qualquer direção.

