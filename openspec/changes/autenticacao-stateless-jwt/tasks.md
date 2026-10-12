# Tasks

## 1. Dependências e configuração

- [x] 1.1 Adicionar `spring-boot-starter-security-oauth2-resource-server` e `spring-boot-starter-security-oauth2-resource-server-test` (escopo `test`) ao `pom.xml`, sem versão. Verificar com `./mvnw dependency:tree` que resolvem para 4.1.1 e trazem `spring-security-oauth2-jose` 7.1.1.
- [x] 1.2 Criar `JwtProperties` (`issuer`, `secret`, `access-token-ttl`, `refresh-token-ttl`) e `RefreshCookieProperties` (`secure`, `same-site`, `partitioned`) em `infra/security/`, conforme design D11 e D14. O construtor de `JwtProperties` rejeita segredo em Base64 inválido ou com menos de 32 bytes, nomeando `APP_JWT_SECRET`. O de `RefreshCookieProperties` rejeita `same-site` desconhecido e `None` com `secure=false`, nomeando as variáveis. Testes unitários:
  - segredo válido, curto e inválido;
  - `Strict`, `lax` e `NONE` aceitos (sem diferenciar maiúsculas);
  - `Foo` rejeitado;
  - `None` com `secure=false` rejeitado;
  - `None` com `secure=true` aceito.
- [x] 1.2a Em `CorsProperties`, rejeitar `*` em `allowed-origins` no boot, nomeando `APP_CORS_ALLOWED_ORIGINS`. Teste unitário com lista válida e com `*`. Verificar que os testes de CORS existentes continuam verdes.
- [x] 1.3 Definir as propriedades nos quatro arquivos:
  - `application.properties`: `issuer=app-adm-obras`, `access-token-ttl=15m`, `refresh-token-ttl=7d`, cookie `secure=${APP_REFRESH_COOKIE_SECURE:true}`, `same-site=${APP_REFRESH_COOKIE_SAME_SITE:Strict}` e `partitioned=${APP_REFRESH_COOKIE_PARTITIONED:false}`, sem segredo;
  - `local`/`test`: segredo fixo de desenvolvimento, comentado como tal, e `secure=false`;
  - `prod`: `secret=${APP_JWT_SECRET}`, sem default, e `secure=true` literal, sem placeholder (design D14).

  Teste no padrão de `OpenApiProfilePropertiesTest`, carregando os arquivos como recurso, que confirme que:
  - `prod` só referencia `APP_JWT_SECRET` e tem `secure=true` literal;
  - `application.properties` não define segredo;
  - o padrão de `same-site` é `Strict`.
- [x] 1.4 Adicionar ao `.env.example`:
  - `APP_JWT_SECRET=` vazio, com comentário sobre o formato (Base64, ≥32 bytes) e o comando `openssl rand -base64 32`;
  - `APP_REFRESH_COOKIE_SAME_SITE=Strict`, com comentário: `None` só para frontend e API em sites diferentes sem proxy;
  - `APP_REFRESH_COOKIE_SECURE=true`, com comentário: ignorado em `prod`;
  - `APP_REFRESH_COOKIE_PARTITIONED=false`.

  Verificar por grep que `APP_JWT_SECRET` está vazio e que os defaults do exemplo batem com os de `application.properties`.

## 2. Domínio, portas e persistência do refresh token

- [x] 2.1 Criar `core/domain/RefreshToken` com `revokeReason` (`ROTATED`, `LOGOUT`, `REUSE`, `USER_CHANGED`), `isActive(now)` e validação de campos obrigatórios (design D3). Testes unitários: token ativo, expirado, revogado e campos nulos. Verificar por grep que `core/` não importa `org.springframework`, `com.nimbusds` nem `jakarta`.
- [x] 2.2 Criar as portas `core/repository/RefreshTokenRepository` e `core/security/AccessTokenIssuer` (com o record `IssuedAccessToken`), conforme D3. Compila sem framework.
- [x] 2.3 Criar `RefreshTokenEntity` (tabela `refresh_token` do D5, com `@OnDelete(CASCADE)` em `user_id`, `token_hash` único, índices em `family_id` e `user_id`), `RefreshTokenJpaRepository` e `RefreshTokenRepositoryAdapter`. `markRotatedIfActive` é um `UPDATE` condicional que retorna as linhas afetadas. Teste de integração com H2:
  - save e `findByHash`;
  - `markRotatedIfActive` devolve `true` na primeira chamada e `false` na segunda;
  - `revokeFamily` não afeta outra família;
  - `revokeAllByUser`;
  - `deleteExpiredBefore`;
  - apagar o usuário apaga os tokens dele.
- [x] 2.4 Verificar se o Flyway do `deploy-viavel` já está no `pom.xml`. Se estiver, criar `V<próximo>__refresh_token.sql` equivalente à entidade e confirmar que a aplicação sobe com `ddl-auto=none` em banco vazio. Se não estiver, registrar no README o DDL a aplicar manualmente em `prod` (design D5) e confirmar que `local` e `test` criam a tabela. Atualização: o Flyway foi instalado depois (grupo 2 do `deploy-viavel`); a tabela passou a ser `V2__refresh_token.sql`, validada em H2 e PostgreSQL 16, e a seção de DDL manual saiu do README.

## 3. Emissão e validação do access token

- [x] 3.1 Em `SecurityConfig` (design D1, D2, D8):
  - beans `JwtEncoder` e `JwtDecoder` HS256 sobre a mesma chave;
  - decoder com validador de `iss`;
  - `JwtAuthenticationConverter` com claim `roles` e prefixo `ROLE_`;
  - `SessionCreationPolicy.STATELESS`;
  - remover `httpBasic`;
  - `permitAll` para login, refresh e logout, mantendo as demais regras, `/h2-console/**` e `frameOptions(sameOrigin)`.

  Remover `AppUserDetailsService` e `AppUserDetailsServiceTest`. Verificar que `SecurityConfigTest` continua verde e que o log do boot em `local` não mostra `Using generated security password`.
- [x] 3.2 Criar `JwtAccessTokenIssuer` (`infra/security/`) implementando `AccessTokenIssuer`, com `iss`, `sub=id`, `roles`, `iat` e `exp = iat + access-token-ttl`, usando um `Clock` injetado. Teste unitário:
  - o token decodificado pelo `JwtDecoder` tem as claims esperadas e `exp - iat = 15 min`;
  - o token não tem e-mail, nome nem hash;
  - um token com payload alterado é rejeitado pelo decoder.
- [x] 3.3 Expor um bean `Clock` (`Clock.systemUTC()`) em `infra/config/` para os use cases e o emissor. Verificar que os testes conseguem substituí-lo.

## 4. Fluxo de autenticação (login, refresh, logout, me)

- [x] 4.1 Ajustar `AuthenticationFailedException`: `unknownEmail` passa a usar a mesma mensagem de `invalidCredentials`, sem o e-mail. Adicionar `invalidRefreshToken()` (`auth.invalid.refresh`), mapeada para 401 no `GlobalExceptionHandler`. Teste: as mensagens de e-mail inexistente e de senha errada são idênticas, e nenhuma contém o e-mail.
- [x] 4.2 Criar `application/.../RefreshTokenFactory` (32 bytes `SecureRandom`, Base64URL, hash SHA-256 hex, só JDK). Teste unitário: 1.000 valores sem repetição, hash determinístico de 64 caracteres, e um valor diferente do seu hash.
- [x] 4.3 Reescrever `AuthUseCase` com `login`, `refresh`, `logout` e `profile(UUID)`, retornando `AuthResult` (design D3, D4). Criar `AuthTokenResponseDTO` (`accessToken`, `tokenType`, `expiresIn`, `user`) e atualizar `UseCaseConfig`. Testes unitários com repositório em memória e `Clock` fixo, cobrindo:
  - login cria uma família com expiração de login + 7 dias;
  - refresh rotaciona e herda a expiração;
  - refresh após a expiração falha;
  - reapresentar um token rotacionado revoga a família, incluindo o token mais novo;
  - outra família continua válida;
  - usuário inativo é recusado;
  - os perfis novos aparecem no access token após o refresh;
  - logout revoga o token;
  - logout com token desconhecido não lança exceção.
- [x] 4.4 Criar o filtro de origem para `POST /api/auth/refresh` e `/logout` (design D7), usando `app.cors.allowed-origins`. Origem ausente ou fora da lista responde 403 sem chegar ao controller. Teste MockMvc: sem `Origin` → 403; `Origin` desconhecida → 403; `http://localhost:3000` → passa.
- [x] 4.5 Atualizar `AuthController`:
  - `login` e `refresh` devolvem `AuthTokenResponseDTO` e o cookie `refresh_token` (design D6), lido com `@CookieValue(required = false)`;
  - `logout` responde 204 e apaga o cookie;
  - refresh recusado apaga o cookie;
  - `me` usa `UUID.fromString(authentication.getName())`.

  Verificar que o corpo nunca contém o refresh token.
- [x] 4.6 Reescrever `AuthControllerIntegrationTest` (`RANDOM_PORT`, perfil `test`, bootstrap) para Bearer e cookie, cobrindo os cenários da spec `token-authentication`:
  - login (sucesso, senha errada, e-mail inexistente com resposta idêntica, inativo, sem corpo);
  - atributos do cookie com a configuração padrão (`HttpOnly`, `SameSite=Strict`, `Path=/api/auth`, sem `Domain`, `Max-Age`);
  - com `same-site=None`, `secure=true` e `partitioned=true` (propriedades sobrescritas no teste): `SameSite=None`, `Secure` e `Partitioned`;
  - cookie de remoção (logout e refresh recusado) com os mesmos atributos e `Max-Age=0`;
  - Bearer válido, ausente, alterado e expirado (com `Clock` de teste);
  - Basic correto → 401;
  - nenhum `Set-Cookie: JSESSIONID`;
  - `OPERADOR` → 403 em escrita;
  - `/me`;
  - refresh e reuso;
  - logout e refresh depois dele;
  - origem não permitida → 403;
  - os testes de CORS existentes.

  `./mvnw test` verde.

## 5. Revogação por mudança no usuário e limpeza

- [x] 5.1 Injetar `RefreshTokenRepository` em `UserUseCase` e revogar todos os tokens do usuário (`USER_CHANGED`) quando a senha for enviada no update ou quando `isActive` passar de `true` para `false` (design D10). Atualizar `UseCaseConfig`. Testes unitários: troca de senha revoga; desativação revoga; só nome não revoga; reativação não revoga. Teste de integração: após `PUT` com senha nova, o refresh com o cookie antigo dá 401.
- [x] 5.2 Criar a limpeza agendada diária de tokens expirados em `infra/` com `@EnableScheduling` (design D12). Teste unitário do componente chamando o método diretamente: só os expirados são removidos.

## 6. Documentação da API e README

- [x] 6.1 Em `OpenApiConfig`, trocar `basicAuth` por `bearerAuth` (`http`, `bearer`, `bearerFormat: JWT`) com requisito global (design D13). Atualizar `OpenApiDocumentationIntegrationTest`:
  - a descrição tem `bearerAuth` com `scheme: bearer` e não tem `basicAuth`;
  - rota sem token dá 401;
  - rota com o token do login dá 200;
  - `OPERADOR` em escrita dá 403.

  Migrar também o `OpenApiDisabledIntegrationTest`, que usava Basic no caso autenticado.
- [x] 6.2 Atualizar o `README.md`:
  - seção "Authentication": fluxo login → Bearer → refresh → logout, validades (15 min / 7 dias absoluta), cookie `HttpOnly` e uso de `credentials: 'include'`, exigência de `Origin` no refresh/logout, e a recomendação de o frontend serializar o refresh (design Risks);
  - "Environment variables": `APP_JWT_SECRET`, `APP_REFRESH_COOKIE_SAME_SITE`, `APP_REFRESH_COOKIE_SECURE` e `APP_REFRESH_COOKIE_PARTITIONED`;
  - seção "Frontend and API on different domains", com a tabela de topologias A/B/C do design D6, a recomendação da topologia B, um exemplo de `rewrites()` do `next.config` encaminhando `/api/:path*` com o prefixo preservado, a configuração da topologia C e o aviso sobre bloqueio de cookies de terceiros;
  - "API documentation": "Authorize" com o access token em vez de Basic.

  Verificar por grep que o README não menciona mais HTTP Basic como forma de autenticação.

## 7. Verificação integrada

- [x] 7.1 Rodar `./mvnw test` completo e confirmar a suíte verde. Verificar por grep que não restam `httpBasic`, `basicAuth` nem `withBasicAuth` em `src/`.
- [ ] 7.2 Verificação manual em `local` com `APP_BOOTSTRAP_ENABLED=true`, via curl com `-H 'Origin: http://localhost:3000'`:
  - login devolve o token e `Set-Cookie`;
  - `GET /api/clients` com Bearer dá 200 e sem Bearer dá 401;
  - o refresh com o cookie devolve um token novo;
  - reapresentar o cookie antigo dá 401;
  - logout dá 204.

  Repetir o login e o "Authorize" no Swagger UI.
- [x] 7.3 Subir com `SPRING_PROFILES_ACTIVE=prod` sem `APP_JWT_SECRET` e confirmar que o boot falha com mensagem que nomeia a variável. Repetir com segredo de 16 bytes e confirmar a mesma falha.
- [x] 7.4 Subir em `local` com `APP_REFRESH_COOKIE_SAME_SITE=None` e `secure=false`, e confirmar que o boot falha nomeando as duas variáveis. Repetir com `APP_CORS_ALLOWED_ORIGINS=*` e confirmar a falha.
- [ ] 7.5 Verificar o atributo `Partitioned` no navegador: com `partitioned=true`, inspecionar o cookie no DevTools de um Chromium atual e registrar no README os navegadores em que foi confirmado. Não afirmar suporte em navegador não testado.

## Workflow follow-up

- Arquivar `documentacao-api` antes de arquivar este change, porque o delta de `api-documentation` exige a spec existente.
- Coordenar com o frontend Next.js a troca de Basic pelo fluxo de tokens antes do deploy (design Migration Plan).
- Definir `APP_JWT_SECRET` no ambiente de produção antes do deploy.
