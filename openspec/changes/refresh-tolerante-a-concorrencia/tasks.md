# Tasks

## 1. Configuração da janela

- [x] 1.1 `JwtProperties` ganha `refreshReuseGrace` (padrão 30 s; rejeita negativo e valor >= `accessTokenTtl`) e `application.properties` declara `app.security.jwt.refresh-reuse-grace=30s`. Verificar por `JwtPropertiesTest` (padrão, `0`, negativo e >= access token).
- [x] 1.2 `UseCaseConfig` passa a janela ao `AuthUseCase`. Verificar pela aplicação subindo com o perfil `local`.

## 2. Repositório

- [x] 2.1 `RefreshTokenRepository.isFamilyClosed(familyId)` (existe token da família com `revoke_reason` diferente de `ROTATED`) no adapter e no JPA. Verificar por `RefreshTokenRepositoryAdapterTest`: família só com `ROTATED` e ativos → aberta; com `LOGOUT`, `REUSE` ou `USER_CHANGED` → fechada.

## 3. Regra de refresh

- [x] 3.1 `AuthUseCase.refresh` aplica a janela ao token `ROTATED` (irmão com mesma expiração se dentro da janela e sessão aberta; senão revoga `REUSE` e 401), relê o token quando `markRotatedIfActive` falha e aplica a mesma regra, e checa usuário ativo também no caminho da janela. Atualizar o Javadoc. Verificar por `AuthUseCaseTest`: reuso em 2 s → novo par e B válido; reuso após 31 s → 401 e B revogado; janela `0` → 401; reuso após logout → 401 sem token; após expiração → 401; usuário inativo → 401 e família revogada; disputa simulada no `markRotatedIfActive` → novo par; expiração do irmão igual à de A.
- [x] 3.2 Teste de integração em `AuthControllerIntegrationTest`: login, refresh com A, refresh de novo com A dentro da janela → 200 e cookie novo; refresh com B e com o novo cookie → 200. Verificar por `./mvnw test`.

## 4. Documentação e validação

- [x] 4.1 README: janela de tolerância, propriedade/variável `APP_SECURITY_JWT_REFRESH_REUSE_GRACE` e rollback com `0`. Verificar a leitura.
- [x] 4.2 `./mvnw test` sem falhas e `openspec validate refresh-tolerante-a-concorrencia --strict` válido.
- [x] 4.3 Teste manual com o front: backend local com `access-token-ttl=60s`, duas abas abertas, navegar após 35 s nas duas ao mesmo tempo → nenhuma volta ao login e o log não mostra refresh recusado.

## Workflow follow-up

- Ao arquivar, ajustar a regra "Reuse of a refresh token revokes its chain" da spec `token-authentication` (change `autenticacao-stateless-jwt`) para remeter à janela de tolerância.
- Publicar nova imagem do backend e atualizar o contêiner de produção.
