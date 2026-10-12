## Why

O caminho "clonar → `docker compose up` contra Postgres externo vazio → fazer login" não funciona hoje, e nenhum change em andamento o fecha sozinho. São três bloqueios em série:

1. **A imagem não constrói.** `.dockerignore` exclui `.mvn`, e o `Dockerfile` faz `COPY .mvn ./.mvn`. O `docker build`, e com ele o `scripts/docker-build-push.sh`, falha antes de compilar.
2. **O schema não existe.** Sem Flyway nem DDL versionado, o perfil `prod` sobe com `ddl-auto=validate` contra um banco vazio e falha no boot.
3. **Ninguém consegue logar.** `application-prod.properties` fixa `app.bootstrap.enabled=false`, e não há outro caminho para criar o primeiro usuário. Mesmo com o schema criado, `app_user` fica vazio.

Este change reúne o mínimo necessário para o primeiro deploy funcionar. Antes, Flyway e a unicidade de refeição estavam em `endurecer-dominio-e-persistencia`. Foram trazidos para cá para que o deploy não espere pela correção do total do projeto e pela otimização de leitura, que continuam naquele change.

## What Changes

### Construção da imagem

- `.dockerignore` deixa de excluir os arquivos do Maven Wrapper que o `Dockerfile` copia. A imagem volta a construir a partir de um contexto limpo.
- O contêiner passa a declarar verificação de prontidão. Esse requisito já está em `app-containerization` (change `infra-docker-compose-e-der`), mas nunca foi implementado.
- O processo Java passa a ser o PID 1 do contêiner e recebe o sinal de parada.

### Gestão de schema (vindo de `endurecer-dominio-e-persistencia`)

- **Flyway** passa a ser o dono do schema. `ddl-auto=none` em todos os perfis.
- Migração baseline `V1` com o schema completo das entidades atuais, índices e a restrição de unicidade de refeição.
- H2 de dev/test em `MODE=PostgreSQL`, com um único conjunto de migrações para todos os ambientes.
- O DER (`app-adm-obras-erd.puml`/`.pdf`) é regerado para refletir o `V1`.

### Integridade de refeição (vindo de `endurecer-dominio-e-persistencia`)

- `UNIQUE (project_id, meal_type)` em `meal`. A violação vinda do banco é traduzida em erro de validação, não em 500.

### Usuário administrador inicial

- O bootstrap do administrador passa a ser habilitável em qualquer perfil, inclusive `prod`, por variável de ambiente. O default continua desligado.
- **BREAKING** (configuração): e-mail e senha do administrador deixam de ter valor padrão fora do perfil `test`. Com o bootstrap habilitado e algum desses valores ausente, a aplicação falha no boot e nomeia a variável. Hoje ela registra um aviso e sobe sem administrador.
- A senha do bootstrap passa pela mesma regra de senha dos usuários criados pela API (abaixo).
- O bootstrap continua idempotente: se o e-mail já existe, nada é alterado, nem a senha.
- O e-mail do perfil `test` passa de `admin@admim.com` para `admin@admin.com`.
- O README ganha o roteiro do primeiro deploy: habilitar, subir, logar, trocar a senha, desabilitar.

### Regra de senha de usuário

- Hoje o mínimo de 6 caracteres é verificado no hash BCrypt (sempre com 60 caracteres), não na senha informada, e por isso nunca rejeita nada. `POST /api/users` aceita senha de 1 caractere.
- **BREAKING** (API): a senha informada passa a ter no mínimo 6 caracteres, de qualquer tipo, em `POST /api/users`, em `PUT /api/users/{id}` quando a senha é enviada, e no bootstrap. Senha curta passa a responder 400 com erro de validação. Usuários já gravados não são afetados.

### Não-objetivos

- Troca obrigatória de senha no primeiro login. Exige estado novo no usuário e fluxo no frontend; o roteiro operacional cobre o risco.
- Correção do total do projeto e leitura em lote de projetos. Continuam em `endurecer-dominio-e-persistencia`.
- Mudança no mecanismo de autenticação.

## Capabilities

### New Capabilities

- `schema-versioning`: o schema do banco é propriedade de migrações versionadas, aplicadas de forma determinística na inicialização, com um único conjunto válido em H2 e PostgreSQL. Spec movida de `endurecer-dominio-e-persistencia`, sem alteração de requisitos.
- `meal-assignment-uniqueness`: um projeto tem no máximo uma refeição de cada tipo, com unicidade garantida pelo banco. Spec movida de `endurecer-dominio-e-persistencia`, sem alteração de requisitos.
- `admin-bootstrap`: provisionamento do primeiro administrador em uma instalação nova, em qualquer ambiente, sem credencial padrão embutida.
- `user-password-policy`: regra mínima aplicada à senha informada pelo usuário, com o mesmo critério em todo ponto que define uma senha.

### Modified Capabilities

(nenhuma; `openspec/specs/` ainda está vazio. Os requisitos de `app-containerization` e `runtime-configuration` que este change ajuda a cumprir estão no change `infra-docker-compose-e-der`, não arquivado, e não mudam.)

## Impact

**Infra de contêiner**
- `.dockerignore`, `Dockerfile` (ENTRYPOINT), `compose.yaml` (healthcheck).
- `pom.xml`: `+spring-boot-starter-actuator`, para a verificação de prontidão.

**Dependências**
- `pom.xml`: `+flyway-core`, `+flyway-database-postgresql`.

**Configuração**
- `application*.properties`: `ddl-auto=none`; URL do H2 com `MODE=PostgreSQL`; bootstrap configurável em `prod`; defaults de e-mail e senha do administrador removidos do perfil base; exposição apenas do endpoint de saúde.
- `.env.example`: variáveis do bootstrap sem senha de exemplo utilizável, com obrigatoriedade condicional documentada.
- Novo `src/main/resources/db/migration/V1__baseline_schema.sql`.

**Código**
- `infra/bootstrap/DataBootstrap.java`: falha no boot por configuração incompleta; validação da senha.
- `core/domain/User.java` e `application/usecase/UserUseCase.java`: validação da senha em claro antes do hash; o check sobre o hash deixa de carregar a regra de tamanho.
- `infra/security/SecurityConfig.java`: endpoint de saúde acessível sem autenticação.
- `infra/entities/MealEntity.java`, `infra/repositories/MealJpaRepository.java`, `infra/adapters/repository/MealRepositoryAdapter.java`, `infra/controllers/GlobalExceptionHandler.java`: unicidade de refeição.

**Documentação**
- `README.md`: roteiro do primeiro deploy; nota sobre bancos com schema criado por `ddl-auto`.
- `app-adm-obras-erd.puml` / `.pdf`: regerados após o `V1`.

**Outros changes**
- `endurecer-dominio-e-persistencia` perde as seções de schema, unicidade e limpeza de portas, e passa a depender deste change.
- `infra-docker-compose-e-der` e `docker-build-push` só podem ser verificados de ponta a ponta e arquivados depois deste change.

**API**
- Sem endpoint de negócio novo. Uma gravação duplicada de refeição que antes resultaria em 500 passa a responder 400. Senha com menos de 6 caracteres em `/api/users` passa a responder 400. `/actuator/health` passa a existir sem autenticação.
