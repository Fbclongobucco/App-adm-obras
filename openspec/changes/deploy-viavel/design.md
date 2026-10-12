## Context

Motivação em `proposal.md`. Requisitos em `specs/`.

**Restrição estrutural.** `core/` e `application/` não importam Spring, jakarta nem javax. Use cases são montados à mão em `infra/config/UseCaseConfig.java`. Nada deste change rompe essa fronteira.

**Construção da imagem.** `Dockerfile` multi-stage, com build em `maven:3.9-eclipse-temurin-25` via `./mvnw` (o wrapper fixa Maven 3.9.16) e runtime em `eclipse-temurin:25-jre`, com usuário `appuser`. `.dockerignore` lista `.mvn`, e o `Dockerfile` faz `COPY .mvn ./.mvn`, então o build falha.

**Schema.** Sem Flyway nem DDL versionado. `ddl-auto`: `update` em `local`, `create-drop` em `test`, `validate` em `prod`. Oito tabelas de entidade (`address`, `cost_center`, `client`, `employee`, `project`, `accommodation`, `meal`, `app_user`), as junções `project_employee` e `accommodation_employee` e a coleção de elemento `user_role`, descritas em `app-adm-obras-erd.puml`.

**Flyway 12.4.0** é a versão gerenciada pelo Spring Boot 4.1.1. Na análise de `endurecer-dominio-e-persistencia`, o conteúdo do jar foi verificado: o suporte a H2 está no `flyway-core`, e o suporte a PostgreSQL exige o módulo `flyway-database-postgresql`.

**Bootstrap.** `DataBootstrap` é um `ApplicationRunner` com `@ConditionalOnProperty(app.bootstrap.enabled=true)`. Com e-mail ou senha vazios, registra aviso e segue. Se o e-mail já existe, não faz nada. A senha vai direto para `passwordHasher.hash()`. `application.properties` define defaults `admin@admim.com`/`admin123`; `application-prod.properties` fixa `enabled=false`.

**Senha.** `User.validatePasswordHash` aplica `MIN_PASSWORD_LENGTH = 6` sobre o **hash**, que no BCrypt tem sempre 60 caracteres, então nunca rejeita. `UserUseCase.create` só barra senha vazia. `UserMapper.toDomain` faz o hash sempre que a senha vem preenchida. `InvalidUserException.shortPassword` já existe e, por ser `CoreDomainException`, o `GlobalExceptionHandler` a traduz em 400.

**Segurança HTTP.** `POST /api/auth/login`, `/error` e os caminhos do springdoc (change `documentacao-api`) liberados; o resto é autenticado.

## Goals / Non-Goals

**Goals:**
- `docker compose up` contra um Postgres externo vazio conclui o boot, cria o schema e permite login do administrador configurado.
- Uma única regra de senha, avaliada sobre a senha informada, em API e bootstrap.

**Non-Goals:**
- Correção de `project.total_price` e leitura em lote de projetos, que ficam em `endurecer-dominio-e-persistencia`.
- Troca obrigatória de senha no primeiro login.
- Expor endpoints do actuator além de `health`.

## Decisions

### D1. Flyway dono do schema, H2 em `MODE=PostgreSQL`, um conjunto de migrações

Trazida de `endurecer-dominio-e-persistencia` (lá, D4), sem mudança de conteúdo.

- `spring-boot-starter-flyway` e `flyway-database-postgresql`, sem versão explícita (herdada do parent). No Boot 4 a autoconfiguração está no módulo `spring-boot-flyway`, trazido pelo starter.
- `spring.jpa.hibernate.ddl-auto=none` em todos os perfis.
- URL do H2 em `local` e `test` com `;MODE=PostgreSQL`.
- `src/main/resources/db/migration/V1__baseline_schema.sql` com o schema completo.

**Por que `MODE=PostgreSQL` em vez de DDL portátil.** O schema usa `uuid`, `boolean`, `numeric(19,2)` e `timestamp`. O modo de compatibilidade alinha esses tipos e mantém um único conjunto de arquivos.

**Por que gerar o `V1` do Hibernate.** Exportar o DDL de `ddl-auto=create-drop` garante que a migração nasce das entidades atuais, e não de um DDL de memória. A partir daí é revisável como código.

**Bancos com schema ad-hoc.** O Postgres deixou de ser provisionado pelo Compose. Um banco externo já usado com `ddl-auto` tem tabelas que o `V1` tenta criar, e o boot falha com "objeto já existe". `spring.flyway.baseline-on-migrate=true` entra **apenas** em `local`; em `prod`, schema não rastreado deve travar o boot. O README documenta como recriar esse banco. Isso substitui a antiga task 1.6 do `endurecer`, que falava do volume `app-adm-obras-pgdata`, removido do Compose.

**Ordem.** O Flyway roda antes do `EntityManagerFactory`, e o `ApplicationRunner` do bootstrap roda depois do contexto pronto. Portanto, schema primeiro e administrador depois, sem configuração extra.

### D2. Unicidade de refeição no banco, com tradução de exceção

Trazida de `endurecer-dominio-e-persistencia` (lá, D5), sem mudança de conteúdo.

- `UNIQUE (project_id, meal_type)` no `V1` e `@Table(uniqueConstraints = ...)` em `MealEntity`.
- `findFirstByProjectIdAndMealType` dá lugar a `findByProjectIdAndMealType` retornando `Optional`.
- `GlobalExceptionHandler` traduz `DataIntegrityViolationException` em erro de validação (400).
- `NULL` é distinto em unicidade no PostgreSQL e no H2 em `MODE=PostgreSQL`, então refeições órfãs (`project_id` nulo) coexistem. Isso precisa de teste explícito.
- `Project.addMeal` continua rejeitando duplicata com mensagem de domínio. O banco é a segunda linha.

### D3. Regra de senha na camada de aplicação, sobre a senha em claro

**Escolha.** Um validador sem framework em `application/` (ex.: `PasswordPolicy.validate(String raw)`), chamado antes do hash em todo ponto que define senha:
- `UserMapper.toDomain`, quando a senha vem preenchida, cobrindo create e update;
- `DataBootstrap`, antes de `passwordHasher.hash()`.

A regra exige no mínimo 6 caracteres de qualquer tipo, decisão do usuário em 2026-10-10. Usa a `InvalidUserException.shortPassword` existente, e o 400 sai pelo handler atual. `User.validatePasswordHash` deixa de checar tamanho e só verifica presença, já que um hash não é senha.

**Por que não validar no domínio `User`.** O domínio recebe só o hash. Passar a senha em claro para dentro de `User` espalharia o segredo por um objeto que é persistido e mapeado. O ponto de transformação é a aplicação, e é ali que a regra fica.

**Alternativa descartada.** Bean Validation (`@Size`) no DTO não cobre o bootstrap e introduziria jakarta em `application/`.

**Usuários existentes.** A regra só roda quando uma senha é definida. Login e leitura de usuários não passam por ela.

### D4. Bootstrap habilitável em prod, sem defaults e com falha rápida

**Escolha.**
- `application-prod.properties`: `app.bootstrap.enabled=${APP_BOOTSTRAP_ENABLED:false}` em lugar do `false` fixo.
- `application.properties`: `app.bootstrap.admin.email=${APP_BOOTSTRAP_ADMIN_EMAIL:}` e `...password=${APP_BOOTSTRAP_ADMIN_PASSWORD:}`, sem default. O nome mantém o default `Administrador`.
- `application-test.properties` mantém credenciais fixas, com o e-mail corrigido para `admin@admin.com`. Testes e documentação que usam `admim` são atualizados.
- `DataBootstrap`: com e-mail ou senha em branco, lança exceção cuja mensagem nomeia `APP_BOOTSTRAP_ADMIN_EMAIL` ou `APP_BOOTSTRAP_ADMIN_PASSWORD`. Exceção em `ApplicationRunner` derruba o boot do Spring Boot.
- `DataBootstrap`: aplica D3 à senha antes do hash.
- O comportamento idempotente (e-mail existente vira no-op) permanece.

**Por que exceção no runner e não `@Validated` nas properties.** A obrigatoriedade é condicional (só quando `enabled=true`), e o bean já é condicional a isso. Validar no runner mantém a regra num lugar só e testável.

**`.env.example`.** As variáveis do bootstrap ficam com placeholders vazios ou explicativos, sem senha utilizável. Isso atende `runtime-configuration` ("Example file carries no real secret"; "Defaults documented in the example are real").

### D5. Correção do build e prontidão do contêiner

- **`.dockerignore`.** Remover a linha `.mvn`. A linha `.mvn/wrapper/maven-wrapper.jar` pode ficar, porque com `distributionType=only-script` o jar não é usado. O wrapper é mantido em vez de chamar o `mvn` da imagem, porque fixa a mesma versão do desenvolvimento (3.9.16).
- **ENTRYPOINT.** Trocar `["sh","-c","java $JAVA_OPTS -jar app.jar"]` por `["java","-jar","app.jar"]`, levando as opções em `JAVA_TOOL_OPTIONS`, que a JVM lê sozinha. A JVM vira PID 1 e recebe o SIGTERM do `docker stop`.
- **Prontidão.** `spring-boot-starter-actuator` com `management.endpoints.web.exposure.include=health` e `management.endpoint.health.show-details=never`; `/actuator/health` liberado sem autenticação no `SecurityConfig`. Healthcheck declarado no `compose.yaml`. Isso cumpre o requisito "reports its own readiness" de `app-containerization`, ainda não implementado.
- **Ferramenta do healthcheck.** Não verifiquei se `eclipse-temurin:25-jre` inclui `curl` ou `wget`. A task 4.3 verifica isso. Se nenhum estiver presente, instalar `curl` no estágio de runtime com `apt-get` e limpeza de cache. Não pode ser usado nada do estágio de build.

### D6. DER regerado após o `V1`

O `.puml` passa a refletir a restrição `UNIQUE (project_id, meal_type)` e é regerado com `./mvnw -Pdiagrams`. A proveniência no diagrama passa a citar o `V1`. Isso cumpre a task 6.2 de `infra-docker-compose-e-der`, que não tinha sido aplicada.

## Risks / Trade-offs

**[`MODE=PostgreSQL` muda comportamento de testes em H2]** → Rodar a suíte antes e depois da troca de URL. Divergência é achado real; não suprimir. Checar tipos de data/hora e ordenação.

**[`V1` diverge das entidades]** → Comparar tabela a tabela o DDL de `create-drop` com o resultado do `V1`. Com `ddl-auto=none`, a divergência aparece em teste de integração, não em produção.

**[BREAKING: senha curta passa a dar 400]** → Um cliente da API que crie usuários com senha de menos de 6 caracteres passa a falhar. Hoje isso é aceito por defeito, não por contrato. Registrado na proposal.

**[Senha do bootstrap fica no `.env` depois do primeiro deploy]** → O roteiro do README manda desabilitar o bootstrap e apagar a senha após o primeiro login e a troca de senha. A idempotência garante que deixar ligado não sobrescreve nada, mas o segredo continua exposto no arquivo.

**[`/actuator/health` público]** → Sem detalhes (`show-details=never`), responde só `{"status":"UP"}`. Nenhum outro endpoint do actuator é exposto.

**[Banco externo com schema legado]** → `baseline-on-migrate` só em `local`. Em `prod`, o boot falha e o README orienta a recriação. Comportamento deliberado.

## Migration Plan

1. Build da imagem: `.dockerignore` e ENTRYPOINT. `docker build` precisa passar antes de qualquer outro passo, para que os seguintes sejam verificáveis em contêiner.
2. Flyway, `ddl-auto=none`, `MODE=PostgreSQL`, `V1` com unicidade. Suíte verde.
3. Unicidade de refeição no código (entidade, repositório, handler). Testes.
4. Regra de senha (D3). Testes de API.
5. Bootstrap (D4). Testes de boot. `.env.example` e README.
6. Actuator e healthcheck.
7. DER regerado.
8. Verificação ponta a ponta: `docker compose up` contra Postgres vazio, login, troca de senha, desabilitar bootstrap, restart.

**Rollback.** Até o primeiro deploy real não há dados a preservar. Depois dele, voltar a `ddl-auto` exige `flyway_schema_history` ignorado. Não recomendado; correções viram `V2`.

## Open Questions

- **Ferramenta do healthcheck** (`curl`/`wget` na imagem JRE). Resolvida na task 4.3, sem efeito em specs ou na estrutura das tasks.
