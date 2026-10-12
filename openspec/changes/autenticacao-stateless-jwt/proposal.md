## Why

A API autentica por HTTP Basic. `POST /api/auth/login` só confere as credenciais e devolve o perfil, sem emitir nada. O cliente precisa reenviar e-mail e senha em toda requisição, e isso tem três consequências:

1. **O frontend guarda a senha.** O Next.js precisa manter a senha em claro durante toda a sessão de uso para montar o cabeçalho `Authorization: Basic`.
2. **Custo por requisição.** Cada chamada recarrega o usuário do banco (`AppUserDetailsService`) e roda o BCrypt de novo.
3. **Não há sessão controlável.** Não existe logout, expiração nem revogação de acesso, só a troca de senha ou a desativação do usuário.

Além disso, a política de sessão não é declarada. O Spring cria `HttpSession` quando precisa, e uma resposta a requisição autenticada já devolveu `JSESSIONID`. A API não é stateless de fato.

## What Changes

- **BREAKING** (API): HTTP Basic deixa de ser aceito. Rotas protegidas passam a exigir `Authorization: Bearer <access token>`.
- **BREAKING** (API): `POST /api/auth/login` passa a devolver, além do perfil, um access token JWT assinado (HS256, 15 min). O refresh token é entregue em cookie `HttpOnly`.
- O servidor deixa de criar e de usar sessão HTTP para autenticação (`SessionCreationPolicy.STATELESS`).
- Novo `POST /api/auth/refresh`: lê o refresh token do cookie e devolve um novo access token e um novo cookie. O refresh token usado é invalidado (rotação).
- Novo `POST /api/auth/logout`: revoga o refresh token do cookie e apaga o cookie.
- O refresh token é opaco e aleatório, e é persistido só como hash. Ele expira, pode ser revogado e, se um token já trocado for reapresentado, todos os tokens daquela cadeia são revogados.
- Desativar um usuário ou trocar a senha dele revoga todos os refresh tokens desse usuário.
- `GET /api/auth/me` passa a identificar o usuário pelo token.
- A chave de assinatura vem de variável de ambiente no perfil `prod`, sem valor padrão. Sem ela, a aplicação falha no boot.
- O Swagger UI (`documentacao-api`) passa a declarar o esquema Bearer em vez de Basic.
- Nova dependência: `spring-boot-starter-security-oauth2-resource-server`, gerenciada pelo parent 4.1.1.

### Decisões tomadas (usuário, 2026-10-11)

- **Validade do access token:** 15 minutos.
- **Validade do refresh token:** 7 dias, absoluta. Conta a partir do login, e a rotação não estende o prazo. Depois disso, novo login.
- **Algoritmo de assinatura:** HS256, com segredo único.
- **Transporte do refresh token:** cookie `HttpOnly`, restrito ao caminho `/api/auth`. O refresh token nunca aparece no corpo das respostas. `/api/auth/refresh` e `/api/auth/logout` passam a ter proteção contra requisição forjada de outra origem.

Os valores ficam em propriedades de configuração, ajustáveis sem mudar código.

**Frontend e API em domínios diferentes** (usuário, 2026-10-11): suportado por configuração. O padrão é `SameSite=Strict`, que atende frontend e API no mesmo site e o proxy same-origin do Next.js. Para sites diferentes sem proxy, usar `SameSite=None` com `Secure` obrigatório e, opcionalmente, `Partitioned`. Configuração incoerente impede a aplicação de subir. O proxy é a topologia recomendada quando os domínios diferem, porque navegadores que bloqueiam cookies de terceiros podem descartar o cookie em sites diferentes.

### Não-objetivos

- Servidor de autorização OAuth2, login social ou SSO.
- Revogação imediata do access token. Ele vale até expirar; a revogação atua sobre o refresh token. O efeito disso é limitado pela validade curta do access token.
- Limite de tentativas de login (rate limiting) e bloqueio de conta.
- Gerenciar sessões por dispositivo (listar ou encerrar sessões específicas).
- Troca obrigatória de senha no primeiro login.

## Capabilities

### New Capabilities

- `token-authentication`: autenticação stateless da API por access token assinado de vida curta, com refresh token rotativo, revogável e persistido só como hash.

### Modified Capabilities

- `api-documentation` (definida no change `documentacao-api`, ainda não arquivado): a interface passa a aceitar o access token Bearer em vez de credenciais HTTP Basic.

## Impact

**Dependências**
- `pom.xml`: `+spring-boot-starter-security-oauth2-resource-server` e `+spring-boot-starter-security-oauth2-resource-server-test` (escopo `test`), sem versão explícita.

**Código**
- `core/`: domínio do refresh token e portas para emissão de access token e persistência de refresh token. Nenhum import de framework.
- `application/`: `AuthUseCase` ganha refresh e logout; `UserUseCase` passa a revogar tokens ao desativar o usuário ou trocar a senha; novos DTOs de resposta de token.
- `infra/security/`: `SecurityConfig` stateless com resource server JWT, sem `httpBasic`; emissor de JWT; conversor de claims para perfis de acesso; propriedades de configuração dos tokens.
- `infra/`: entidade, repositório JPA e adaptador do refresh token.
- `infra/controllers/AuthController`: novos endpoints `refresh` e `logout`.
- `infra/config/OpenApiConfig`: esquema Bearer.
- `AppUserDetailsService` deixa de ser usado no fluxo de requisição e é removido se nada mais depender dele.

**Banco**
- Nova tabela de refresh tokens. Se o Flyway do change `deploy-viavel` já estiver aplicado, a tabela entra numa migração nova; senão, pelo `ddl-auto` atual.

**Configuração**
- Novas propriedades `app.security.jwt.*` (validades, segredo HS256, emissor) e `app.security.refresh-cookie.*` (`secure`, `same-site`, `partitioned`). Em `prod`, chave obrigatória por variável de ambiente e `secure` sempre ligado. Em `local` e `test`, chave de desenvolvimento.
- Validação no boot da configuração do cookie e da lista de origens CORS.
- `.env.example`: `APP_JWT_SECRET`, `APP_REFRESH_COOKIE_SAME_SITE`, `APP_REFRESH_COOKIE_SECURE` e `APP_REFRESH_COOKIE_PARTITIONED`, com explicação.

**Testes**
- `AuthControllerIntegrationTest` e `OpenApiDocumentationIntegrationTest` usam Basic e precisam ser reescritos para Bearer.
- `SecurityConfigTest` (com `@WithMockUser`) continua válido para as regras de autorização.

**Clientes**
- O frontend Next.js precisa trocar o envio de Basic pelo fluxo login → Bearer → refresh → logout, guardando o access token só em memória e chamando `/api/auth/*` com `credentials: 'include'`. Mudança obrigatória para o cliente continuar funcionando.

**Documentação**
- `README.md`: fluxo de autenticação e uso do token no Swagger UI.

**Relação com outros changes**
- `documentacao-api`: arquivar antes deste, porque este change modifica a capability `api-documentation`.
- `deploy-viavel`: aplicar antes é preferível, para a tabela nova entrar como migração Flyway. Não é bloqueante.
- `endurecer-dominio-e-persistencia`: a proposal dele registra a troca de autenticação como change próprio. Este é esse change.
