# Tasks

## 1. Build da imagem

- [ ] 1.1 Remover a linha `.mvn` do `.dockerignore`, mantendo a exclusão de `.mvn/wrapper/maven-wrapper.jar`. Verificar que `docker build -t app-adm-obras:check .` conclui a partir de um clone limpo.
- [ ] 1.2 Trocar o ENTRYPOINT do `Dockerfile` para a forma exec `["java","-jar","app.jar"]` e mover as opções de memória para `JAVA_TOOL_OPTIONS`. Verificar com `docker exec <ctr> cat /proc/1/cmdline` que o PID 1 é a JVM, e que `docker stop` encerra o contêiner em menos de 10 s, com o log de shutdown do Spring.

## 2. Flyway e baseline do schema

- [x] 2.1 Adicionar `spring-boot-starter-flyway` e `flyway-database-postgresql` ao `pom.xml` sem versão explícita. (No Spring Boot 4 a autoconfiguração do Flyway fica no módulo `spring-boot-flyway`, trazido pelo starter; só `flyway-core` não a ativa.) Verificar com `./mvnw dependency:tree` que ambas resolvem para a versão gerenciada pelo parent.
- [x] 2.2 Definir `spring.jpa.hibernate.ddl-auto=none` em `application.properties`, `application-local.properties`, `application-prod.properties` e `application-test.properties`, removendo `update`, `create-drop` e `validate`. Verificar por grep que nenhum perfil tem outro valor.
- [x] 2.3 Adicionar `;MODE=PostgreSQL` à URL do H2 em `application-local.properties` e `application-test.properties`. Rodar a suíte antes e depois. Toda divergência deve ser investigada como achado real, sem suprimir teste.
- [x] 2.4 Definir `spring.flyway.baseline-on-migrate=true` apenas em `application-local.properties`. Verificar por grep que `prod` e `test` não a têm.
- [x] 2.5 Gerar o DDL de referência com `ddl-auto=create-drop` em banco limpo e criar `src/main/resources/db/migration/V1__baseline_schema.sql` com tipos válidos em H2 `MODE=PostgreSQL` e em PostgreSQL. Incluir:
  - todas as tabelas, junções e `user_role`;
  - as FKs;
  - índices nas FKs consultadas e nas tabelas de junção;
  - `project.os` único;
  - `UNIQUE (project_id, meal_type)` em `meal`.

  Achado na implementação: no H2 2.4.240, `CHECK (col IN (...))` com dois ou mais valores falha com "Check constraint invalid" depois que a conexão que criou a tabela é fechada. As colunas de enum ficam `varchar` sem esse `CHECK`, e as entidades declaram `@JdbcTypeCode(SqlTypes.VARCHAR)` para o `H2Dialect` não usar o tipo `ENUM` nativo. A tabela `refresh_token` (change `autenticacao-stateless-jwt`) entra como `V2__refresh_token.sql`.
- [x] 2.6 Comparar tabela a tabela, coluna a coluna, o schema do `create-drop` com o do `V1`, e resolver toda divergência. Verificar que a aplicação sobe em banco vazio aplicando o `V1`, e que um segundo boot no mesmo banco não reaplica nada nem perde dados (teste de integração).
- [x] 2.7 Adicionar ao README uma nota: banco externo já usado com `ddl-auto` precisa ser recriado (ou ter o schema removido) antes do primeiro boot com Flyway em `prod`; em `local`, `baseline-on-migrate` cobre o caso. Verificar que a nota não cita serviço nem volume Postgres no Compose.

## 3. Unicidade de refeição

- [ ] 3.1 Declarar `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "meal_type"}))` em `MealEntity`, espelhando o `V1`. Verificar que o boot com `ddl-auto=none` não acusa divergência.
- [ ] 3.2 Substituir `findFirstByProjectIdAndMealType` por `findByProjectIdAndMealType` retornando `Optional` em `MealJpaRepository` e no adaptador. Verificar que os testes de leitura de projeto continuam verdes.
- [ ] 3.3 Traduzir `DataIntegrityViolationException` para a resposta de erro de validação em `GlobalExceptionHandler`. Verificar por teste de integração que gravar duas refeições do mesmo tipo no mesmo projeto, contornando a aplicação, responde 400 e não 500.

  > **Atendida por `auditoria-restaurante-e-conflitos` (tarefas 3.3 e 5.4), com mudança de código de resposta:** a violação é traduzida pelo nome da restrição para **409**, não 400. Duplicidade de refeição responde 409 `meal.duplicate.type`. Os cenários de `meal-assignment-uniqueness` que dizem "erro de validação" passam a ser verificados com esse 409. Marcar esta tarefa quando aquele change concluir a 5.4.
- [ ] 3.4 Testar que várias refeições com `project_id` nulo coexistem sem violar a restrição.
- [ ] 3.5 Testar a troca de almoço e janta numa única alteração (aceita) e o conjunto final com dois almoços (rejeitado, mantendo o conjunto anterior).

## 4. Regra de senha, bootstrap e prontidão

- [ ] 4.1 Criar o validador de senha em `application/`, sem framework: mínimo de 6 caracteres de qualquer tipo, usando `InvalidUserException.shortPassword`. Chamá-lo em `UserMapper.toDomain` antes do hash, e remover o check de tamanho de `User.validatePasswordHash`, que passa a verificar só presença. Testes de API:
  - `POST /api/users` com 5 caracteres dá 400 e não cria usuário;
  - com 6 caracteres dá 201;
  - `PUT` com senha curta dá 400 e mantém a senha anterior;
  - `PUT` sem senha mantém a atual.

  Verificar também que um usuário gravado com hash de senha curta continua autenticando.
- [ ] 4.2 Ajustar o bootstrap (design D4):
  - `app.bootstrap.enabled=${APP_BOOTSTRAP_ENABLED:false}` em `application-prod.properties`;
  - e-mail e senha sem default em `application.properties`;
  - e-mail `admin@admin.com` em `application-test.properties`, atualizando os testes que usam `admim`;
  - em `DataBootstrap`, falha no boot nomeando `APP_BOOTSTRAP_ADMIN_EMAIL` ou `APP_BOOTSTRAP_ADMIN_PASSWORD` quando ausentes;
  - aplicação do validador da 4.1 antes do hash.

  Testes:
  - boot habilitado sem senha falha com o nome da variável;
  - boot habilitado sem e-mail falha com o nome da variável;
  - boot com senha de 5 caracteres falha sem criar usuário;
  - e-mail já existente com senha diferente mantém a senha atual;
  - reinícios repetidos mantêm um único usuário.
- [ ] 4.3 Adicionar `spring-boot-starter-actuator`, expor só `health` com `show-details=never` e liberar `/actuator/health` no `SecurityConfig`. Verificar se `eclipse-temurin:25-jre` tem `curl` ou `wget`; se não tiver, instalar `curl` no estágio de runtime. Declarar o healthcheck no `compose.yaml`. Testar:
  - `GET /actuator/health` anônimo responde 200 `{"status":"UP"}`;
  - `/actuator/env` e `/actuator/beans` não existem;
  - `docker compose ps` mostra o serviço `healthy`.
- [ ] 4.4 Atualizar `.env.example`: variáveis do bootstrap sem senha utilizável, obrigatoriedade condicional (só com `APP_BOOTSTRAP_ENABLED=true`) e defaults iguais aos reais. Verificar contra os placeholders dos `application*.properties`.
- [ ] 4.5 Adicionar ao README o roteiro do primeiro deploy:
  1. `.env` com `APP_BOOTSTRAP_ENABLED=true`, e-mail e senha;
  2. `docker compose up`;
  3. login;
  4. troca de senha via `PUT /api/users/{id}`;
  5. `APP_BOOTSTRAP_ENABLED=false` e senha removida do `.env`;
  6. restart.

  Verificar que cada passo funciona como escrito (task 6.1).

## 5. Diagrama do schema

- [ ] 5.1 Atualizar `app-adm-obras-erd.puml` com `UNIQUE (project_id, meal_type)` e proveniência citando o `V1`, e regerar o PDF com `./mvnw -Pdiagrams`. Verificar que ambos os arquivos mudaram e que o diagrama bate com o `V1`.

## 6. Verificação de conjunto

- [ ] 6.1 Ponta a ponta contra um Postgres externo vazio: seguir o roteiro da 4.5 com `docker compose up --build`. Conferir que o boot aplica o `V1`, cria o administrador, que o login funciona, que a troca de senha funciona e que, após desabilitar o bootstrap e reiniciar, o login funciona só com a senha nova.
- [ ] 6.2 Rodar `./mvnw test` e confirmar a suíte completa verde.
- [ ] 6.3 Validar com `npx @fission-ai/openspec validate deploy-viavel --strict`, e também `endurecer-dominio-e-persistencia`, ajustado por este change.

## Workflow follow-up

- Depois de aplicado este change, marcar as tasks de `infra-docker-compose-e-der` e `docker-build-push` que passarem a ser verificáveis ponta a ponta, e arquivar os dois.
- Arquivar `deploy-viavel` antes de `endurecer-dominio-e-persistencia`, que depende dele.
