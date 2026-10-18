## Context

Motivação em `proposal.md`. Estado atual relevante:

- Spring Boot 4.1.1, Hibernate 7.x, Java 25, Tomcat embarcado. `spring.jpa.open-in-view=false`.
- Transações só existem nos adaptadores (`infra/adapters/repository/*`, `@Transactional`). Casos de uso (`application/usecase`) não abrem transação e não importam Spring.
- O domínio não usa `record` nem `enum` solto (commit `a36aa88`). DTOs em `application/dto` são `record`.
- O mapeamento entidade → domínio acontece dentro dos métodos `@Transactional` dos adaptadores.
- Autenticação: JWT HS256. O `subject` é o id do usuário; os papéis vão no claim `roles`. Não há e-mail nem nome no token.
- Testes de integração: `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`, perfil `test`, H2 com `MODE=PostgreSQL`, apoio em `support/AdminApi`.
- `PageRequest.MAX_SIZE = 100`.
- Não há PostgreSQL no `compose.yaml` (só o app). Não há Actuator.
- O perfil `local` usa H2 em memória, com console do H2 e `baseline-on-migrate`. O `spring-boot-docker-compose` está no `pom.xml`, mas desligado em todos os perfis.

## Goals / Non-Goals

**Goals:**

- Uma estratégia de carga única para todos os adaptadores, sem um caso especial por tela.
- Auditoria de alterações sem tocar nos casos de uso nem no domínio.
- Teste de carga que roda só com Docker e produz números comparáveis entre os dois modos de thread.

**Non-Goals:**

- Trocar Spring Data JPA por outra camada de acesso, ou escrever SQL nativo nas leituras.
- Projeções/DTOs de leitura dedicados por tela.
- Ajuste fino de JVM (GC, heap) além do necessário para executar o teste de carga.

## Decisions

### D1. Virtual threads pela propriedade do Spring Boot

`spring.threads.virtual.enabled=${APP_VIRTUAL_THREADS:true}` em `application.properties`. O Boot passa a usar virtual threads no Tomcat e no `@Scheduled` (`RefreshTokenCleanup`).

`spring.datasource.hikari.maximum-pool-size=${DB_POOL_MAX_SIZE:10}`. O 10 é o padrão do Hikari, mantido.

- **Por que não um `Executor` próprio:** a propriedade cobre Tomcat, agendamento e `@Async` de forma consistente, e é desligável por ambiente, como pede a comparação.
- **Pinning:** o Java 25 já não fixa a virtual thread em `synchronized` (JEP 491). Pinning residual (código nativo, `Object.wait` em biblioteca) é verificado na carga pelo evento JFR `jdk.VirtualThreadPinned`, não presumido.
- **Pool como limite:** com virtual threads, a concorrência efetiva no banco é o pool. O teste de carga registra o tamanho do pool usado.
- **Teste do modo:** um controller só de teste (`src/test`) devolve `Thread.currentThread().isVirtual()`. Dois testes de contexto, um com a propriedade padrão e outro com `APP_VIRTUAL_THREADS=false`.

### D2. Contagem de consultas por `StatementInspector`

O teste de contagem registra `support/QueryCounter` como `hibernate.session_factory.statement_inspector`, só na própria classe de teste (`@SpringBootTest(properties=...)`). O inspetor conta os `SELECT` preparados durante a chamada HTTP e guarda o SQL para a mensagem de falha. Cada teste compara página 5 × página 50 (e detalhe e gravação com 2 × 30 vínculos), e a falha traz endpoint, contagens e consultas.

- **Por que só `SELECT`:** gravar uma obra com 30 colaboradores faz 30 `INSERT` em `project_employee` por natureza; isso não é N+1. As estatísticas do Hibernate (`getPrepareStatementCount`, plano inicial) misturam leitura e escrita e não servem para o requisito de escrita.
- **Banco próprio:** a classe usa um H2 em memória com nome próprio. A massa é uniforme e dados de outras classes de teste não alteram a contagem.
- **Alternativa:** `datasource-proxy`. Rejeitada por exigir dependência nova.
- **Limite conhecido:** gravações JDBC fora do Hibernate (D5, D6) não entram na contagem. Elas não fazem parte das leituras medidas.
- A massa de cada teste é criada pela API (`AdminApi`), como nos testes existentes.

### D3. Tudo `LAZY`, carga em lote global

- Todas as associações `@ManyToOne`, `@ManyToMany`, `@OneToMany` e `@ElementCollection` das entidades passam a `LAZY`.
- `spring.jpa.properties.hibernate.default_batch_fetch_size=100` (= `PageRequest.MAX_SIZE`): o acesso a uma associação não carregada busca de uma vez, por `IN (...)`, a associação de todas as entidades do mesmo tipo na sessão.
- O `@BatchSize(100)` local de `MealEntity.adjustments` fica redundante e sai.
- Cargas que não são associação mapeada passam a ser em lote, explícitas no adaptador:
  - refeições da página de obras: `MealJpaRepository.findByProjectIdIn`, agrupadas por obra;
  - obras da página de clientes: `ProjectJpaRepository.findByClientIdIn`, agrupadas por cliente.
- Escrita: `applyRelations` (obra) e o vínculo de colaboradores (hospedagem) usam `findAllById(ids)`, uma consulta por lista.

Alternativas:

- **`JOIN FETCH` / `@EntityGraph` na consulta da página:** com coleção e paginação, o Hibernate pagina em memória (aviso HHH90003004) e gera produto cartesiano entre coleções. Fica restrito a associação to-one, e só se a medição mostrar ganho sobre o lote.
- **Manter `EAGER` com lote:** resolve a contagem, mas mantém o grafo arrastado (refeição → restaurante → obra → colaboradores → centro de custo) em toda leitura de refeição.
- **`@BatchSize` por associação:** mesmo efeito com mais pontos de manutenção. Um valor global é suficiente.

Risco de `LazyInitializationException`: todo método público de adaptador que mapeia para domínio precisa ser `@Transactional`. A tarefa de revisão lista os métodos; os testes de contagem serializam cada resposta.

### D4. Índices por medição, em PostgreSQL

Processo:

1. Massa do cenário `grande` (D7) em PostgreSQL: perfil `local` (D11) ou ambiente `perf/`.
2. `EXPLAIN (ANALYZE, BUFFERS)` de cada consulta candidata, capturada com `hibernate.show_sql`/log do PostgreSQL durante o roteiro.
3. Índice criado na sessão.
4. Novo plano. Só entra na migração o que melhorar o plano.
5. Consulta e planos antes e depois registrados em `perf/RESULTADOS.md`.

Candidatos, a medir:

- `refresh_token(expires_at)`: limpeza diária.
- `project(created_at)`, `project(start_date)`, `project(os)` como ordenação: o `sort` aceita esses campos; `os` já tem índice único.
- Ordenação padrão das demais listagens.
- `findByProjectIdIn` das refeições: `uk_meal_project_lunch` já começa por `project_id`.
- Busca `q`: `translate(lower(col)) LIKE '%termo%'` não usa B-tree. O índice útil seria trigram (`pg_trgm`, GIN sobre a expressão), exclusivo do PostgreSQL.

Se o trigram se mostrar necessário, ele vai em local Flyway por fornecedor: `spring.flyway.locations=classpath:db/migration,classpath:db/vendor/{vendor}`, arquivo em `db/vendor/postgresql/`. Assim o H2 segue só com as migrações compartilhadas. A versão do arquivo exclusivo não pode colidir com as compartilhadas. Sem ganho medido, nada disso é criado.

Os índices das tabelas de auditoria seguem os filtros das specs e entram no `V9` sem medição prévia (ver D5 e D6).

**Resultado da medição (2026-10-18).** A busca `q` de colaboradores foi a consulta de maior custo total no cenário `grande`. Com índice trigram, ela cai de 49,6 para 1,6 ms, mas só com as constantes do `translate` literais no SQL; o Hibernate as envia como parâmetros, e no plano genérico o índice não é usado. Nenhum índice entrou neste change. O trigram exige mudar a geração do SQL e criar `pg_trgm` em produção, e fica para um change próprio (decisão do usuário). Detalhes em `perf/RESULTADOS.md`.

### D5. Auditoria de alterações por listener do Hibernate

Um componente de `infra` registra listeners `PostInsert`, `PostUpdate` e `PostDelete` no `EventListenerRegistry` da `SessionFactory`, na subida.

- **Tipos auditados:** mapa fechado classe da entidade → tipo, com as dez entidades listadas em `change-audit-log`. `AddressEntity`, `RefreshTokenEntity` e as próprias tabelas de auditoria ficam fora.
- **Gravação:** dentro do listener, por `session.doWork(connection -> PreparedStatement)`, na mesma conexão e transação do flush. A alteração e o evento confirmam ou desfazem juntos.
- **Por que não `TransactionSynchronization.beforeCommit` + `JdbcTemplate`:** o `JpaTransactionManager` dispara `beforeCommit` antes do flush do commit. Entidades gravadas só no flush final ficariam sem evento.
- **Por que não `session.persist` de uma entidade de auditoria no listener:** alterar a fila de ações durante o flush não é suportado.
- **Autor:** `SecurityContextHolder` → `Authentication.getName()` (id do usuário). Nome e e-mail vêm de uma consulta a `app_user` pela mesma conexão, feita uma vez por transação e guardada como recurso da transação (`TransactionSynchronizationManager`). Sem autenticação: `actor_id` nulo, `actor_name = 'SYSTEM'`.
  - Alternativa rejeitada: pôr nome e e-mail no JWT. O token valeria por 15 min com dados possivelmente desatualizados, e o conteúdo do token mudaria.
- **Instante:** do `Clock` da aplicação (`ClockConfig`), testável.
- **Sem eventos falsos:** `PostUpdate` só dispara para entidade suja. Requisição que não grava não gera evento. Alteração só de vínculo (colaboradores da obra) já toca `updatedAt` no caso de uso e aparece como `UPDATE` da obra.
- **Cobertura:** atualizações em massa por JPQL (`@Modifying`) não passam pelo listener. Hoje só existem em `refresh_token`, fora do escopo. Exclusões em cascata pelo JPA (`cascade = ALL`, `orphanRemoval`) geram evento por filho. `on delete cascade` puro do banco não geraria; uma tarefa confere que os filhos auditados são removidos pelo JPA.
- **Por que não chamar uma porta nos casos de uso:** são dez casos de uso com três operações cada, fácil de esquecer, e sem transação compartilhada com a gravação. O domínio e `application/` ficam intactos.

### D6. Auditoria de login no caso de uso

- Domínio: classe `LoginRecord` (sem `record` Java, conforme convenção) e porta `LoginAuditRepository` em `core/repository`.
- `AuthUseCase.login(dto, LoginOrigin origin)`, onde `LoginOrigin` é DTO `record` com IP e user-agent. `AuthController.login` monta a origem a partir de `HttpServletRequest.getRemoteAddr()` e do cabeçalho `User-Agent` (truncado em 512).
- **Ordem:** autenticar → gravar registro → emitir refresh/access token. Falha no registro aborta o login sem sessão, como pede a spec.
- **IP atrás de proxy:** `getRemoteAddr()` só reflete o cliente se o proxy for confiável e `server.forward-headers-strategy=native` estiver ligado. Fica configurável por ambiente (`SERVER_FORWARD_HEADERS_STRATEGY`) e desligado por padrão, porque aceitar `X-Forwarded-For` sem proxy permite falsificar o IP.

### D7. Teste de carga em `perf/`

- `perf/compose.yaml`:
  - `postgres` (versão fixa);
  - `app` (o `Dockerfile` existente, perfil `prod`, `APP_VIRTUAL_THREADS` e `DB_POOL_MAX_SIZE` vindos do ambiente);
  - `k6` (`grafana/k6`, versão fixa).
- `perf/scenarios.json`: quantidade por tipo de cadastro em cada cenário. Valores iniciais **nominais**, sem relação com produção, ajustáveis no arquivo:
  - `pequeno`: 20 obras, 100 colaboradores;
  - `medio`: 200 obras, 1.000 colaboradores;
  - `grande`: 2.000 obras, 10.000 colaboradores.

  Os demais tipos (centros de custo, clientes, restaurantes, refeições, hospedagens, usuários) crescem na mesma proporção, definida no arquivo.
- `perf/seed.js`: script k6 (1 VU, 1 iteração) que faz login como ADMIN e cria a massa pelos endpoints, na ordem de dependência da spec. CPF e CNPJ válidos são gerados no script, com o mesmo algoritmo de `support/FakeDocuments`. Para no primeiro cadastro rejeitado, informando endpoint e resposta.
  - **Por que pela API e não por SQL:** a massa passa pelas validações e regras do domínio (totais, vínculos, coberturas de refeição) e não depende de conhecer o schema. O custo é tempo de carga maior, aceito.
  - **Efeito colateral:** cada cadastro gera evento de auditoria (D5), como no uso real. A massa de auditoria fica proporcional à de negócio.
  - **Por que k6 também para a massa:** uma ferramenta só, sem dependência nova.
- `BASE_URL`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` e `SCENARIO` por variável de ambiente.
  - Alvo local: admin do bootstrap.
  - Alvo `perf/`: perfil `prod`, com bootstrap sempre desligado. Um SQL mínimo (`perf/admin.sql`) cria só o usuário ADMIN de carga, com hash BCrypt fixo, antes da massa.
- `perf/script.js`, roteiro k6:
  - login uma vez por usuário virtual (o BCrypt do login não deve dominar a medição);
  - mix de listagens (tamanho 20), detalhes, busca `q` e uma escrita (criar e excluir centro de custo, com CNPJ válido gerado no roteiro);
  - `VUS` e `DURATION` parametrizáveis;
  - duração máxima abaixo da validade do access token (15 min), ou refresh no roteiro.
- `perf/run.sh --target local|perf --scenario <nome> --threads on|off`:
  - `perf`: sobe o ambiente, espera o login responder, cria o admin e a massa, roda o roteiro e derruba com `down -v`;
  - `local`: não sobe nada. Exige a aplicação já rodando no perfil `local`, com o modo de thread informado ao subir. Cria a massa só se a base estiver vazia, para não duplicar entre execuções;
  - resumo em `perf/results/<data>-<alvo>-<cenario>-<modo>.json`, gravado pelo `handleSummary` do roteiro com as condições da execução (`meta`) e as métricas por grupo; o script falha se o arquivo não for gravado. JFR opcional por `JAVA_OPTS` para o evento de pinning.
- O k6 roda pela imagem `grafana/k6` nos dois alvos, com o UID/GID do host para gravar o resumo em `perf/results`. No alvo local, usa `--network host`: o caminho por `host.docker.internal` foi bloqueado pelo firewall do host (Fedora) na verificação. `--network host` vale para Linux.
- `perf/RESULTADOS.md`: tabela por execução com condições (data, commit, alvo, cenário, VUs, duração, pool, CPU/RAM da máquina) e métricas por grupo de endpoint. Só valores medidos. Proposta não medida fica marcada como hipótese. Volumes declarados como nominais.
- **Uso dos alvos:** `local` é o ciclo rápido de desenvolvimento e de análise de plano (D4). `perf/` isola a aplicação em contêiner com recursos fixos e é a referência para a comparação on/off registrada no relatório.
- **Por que k6 e não Gatling:** decisão do usuário. Roda por imagem, sem dependência no `pom.xml`.

### D8. Schema `V9`

- `change_audit`:
  - `id uuid` PK;
  - `entity_type varchar(30)`, `entity_id uuid`, `action varchar(10)`, `occurred_at timestamp(6) with time zone`, todos not null;
  - `actor_id uuid` nulo, sem FK: o usuário pode ser excluído e o evento permanece;
  - `actor_name varchar(255)` not null, `actor_email varchar(255)`.
  - Índices: `(entity_type, entity_id, occurred_at)`, `(actor_id, occurred_at)`, `(occurred_at)`.
- `login_audit`:
  - `id uuid` PK;
  - `user_id uuid` not null, sem FK;
  - `user_name`, `user_email`, `occurred_at` not null;
  - `ip_address varchar(45)` (cobre IPv6), `user_agent varchar(512)`.
  - Índices: `(user_id, occurred_at)`, `(occurred_at)`.
- Sem `CHECK` de valores, pelo mesmo motivo do `V1` (H2).
- Ids gerados em Java (`UUID.randomUUID()`), porque gerar UUID em SQL não é portável entre H2 e PostgreSQL.
- Índices medidos em D4 entram no mesmo `V9` ou num `V10`, conforme a ordem de implementação.

### D9. Consulta da auditoria

- `AuditQueryUseCase` em `application/usecase`, com portas `ChangeAuditRepository` e `LoginAuditRepository` (leitura paginada com filtro).
- Ordenação fixa: `occurred_at desc, id desc`.
- Filtro inválido (tipo desconhecido, `from > to`) lança exceção de domínio → 400 com `errorCode`, pelo `GlobalExceptionHandler` existente. Instante malformado cai no `MethodArgumentTypeMismatchException` já tratado.
- `AuditController` em `/api/audit`. `SecurityConfig` ganha `.requestMatchers("/api/audit/**").hasRole("ADMIN")` **antes** da regra `GET /api/**`, que libera OPERADOR.
- O Swagger (`documentacao-api`) documenta os dois endpoints.

### D10. Sobreposição com `endurecer-dominio-e-persistencia`

O grupo 2 de tarefas e a spec `project-read-performance` daquele change estão desatualizados: citam `toDomain(entity, lunch, dinner)`, que não existe desde o `V4`. Este change os absorve. Na implementação:

- marcar o grupo 2 de lá como substituído, com referência a este change;
- remover a spec `project-read-performance` de lá, para não gerar spec principal duplicada no arquivamento.

O grupo 1 (total do projeto) não é tocado.

### D11. Perfil `local` com PostgreSQL pelo Docker Compose do Spring Boot

- O `spring-boot-docker-compose` já está no `pom.xml` (runtime, opcional) e hoje está desligado em todos os perfis.
- Novo `compose.local.yaml` na raiz, só com o serviço `postgres` (imagem com versão fixa, a mesma de `perf/compose.yaml`) e volume nomeado para os dados. O `compose.yaml` existente continua sendo o de deploy da aplicação e não muda.
- `application-local.properties`:
  - `spring.docker.compose.enabled=true`;
  - `spring.docker.compose.file=compose.local.yaml`;
  - `spring.docker.compose.lifecycle-management=start-only`: o contêiner continua de pé entre reinícios e os dados persistem;
  - saem a URL do H2, o driver, as credenciais `sa` e o console do H2.

  O Boot obtém URL e credenciais do contêiner (service connection). Nada fica fixo no arquivo.
- `spring.flyway.baseline-on-migrate=true` sai do `local`: o banco PostgreSQL local nasce vazio e é migrado do `V1`.
- Bootstrap do admin continua ligado no `local`. `DataBootstrap` já não recria o admin se o e-mail existe.
- Perfil `test` e `prod` não mudam. A suíte continua em H2, sem Docker.
- **Alternativa:** perfil novo (`local-pg`), mantendo o H2 no `local`. Rejeitada por decisão do usuário (2026-10-18): o `local` passa a ser PostgreSQL.
- **Alternativa:** subir o PostgreSQL à mão (`docker run`) com URL fixa no perfil. Rejeitada: o Docker Compose do Boot já está no projeto e evita porta e senha fixas.
- Para zerar a base local: `docker compose -f compose.local.yaml down -v`, documentado no README.

## Risks / Trade-offs

- **`LAZY` em caminho não coberto gera `LazyInitializationException`** → revisão de `@Transactional` em todos os métodos públicos de adaptador; testes de contagem serializam todas as listagens e detalhes.
- **Lote de 100 gera `IN` com até 100 parâmetros** → volume aceito por PostgreSQL e H2; o valor é uma propriedade.
- **Listener de auditoria acrescenta um INSERT por registro gravado e um SELECT de autor por transação** → custo medido no teste de carga (grupo de escrita); se relevante, o autor passa a vir de cache.
- **Exclusão em cascata pelo banco não gera evento** → tarefa verifica que os filhos auditados são removidos via JPA.
- **Registro de login gravado e emissão de token falhando em seguida** deixa um login sem sessão → aceito; ocorre só em falha de banco entre duas gravações.
- **Virtual threads com pool pequeno podem aumentar a espera por conexão sob carga** → o pool é configurável e o teste registra o valor usado; o padrão só muda se a medição justificar.
- **Resultado de carga depende da máquina** → o relatório registra os recursos, e a comparação vale só entre execuções na mesma máquina, no mesmo alvo e no mesmo cenário.
- **No alvo `local`, aplicação, banco e k6 disputam a mesma máquina, sem limite de recursos** → serve para ciclo rápido e análise de plano; a comparação on/off de referência é feita no alvo `perf/`.
- **Massa pela API é lenta no cenário `grande`** → criada uma vez; no alvo `local` é reaproveitada entre execuções (dados persistem).
- **Perfil `local` passa a exigir Docker** → mudança no fluxo de desenvolvimento, documentada no README; a suíte de testes não é afetada.
- **IP e user-agent são dados pessoais (LGPD)** → acesso só ADMIN; retenção em aberto.
- **Tabelas de auditoria crescem sem limite** → índices por `occurred_at` deixam um expurgo futuro barato; o expurgo é não-objetivo.

## Migration Plan

1. Deploy com `V9`. A migração é aditiva: só tabelas novas e, se medidos, índices novos.
2. Virtual threads entram ligadas. Rollback operacional: `APP_VIRTUAL_THREADS=false`, sem novo deploy de código.
3. Rollback de código: as tabelas de auditoria ficam no banco sem uso; nenhuma coluna existente muda.
4. A mudança `EAGER` → `LAZY` não altera schema nem contrato; rollback é reverter o código.

## Open Questions

- **Há proxy reverso na frente da API em produção?** Define se `SERVER_FORWARD_HEADERS_STRATEGY=native` deve ser ligado no deploy.
- **Retenção dos registros de auditoria** (LGPD): prazo e responsável pelo expurgo.
- **Versão do PostgreSQL de produção**, para fixar a mesma em `perf/compose.yaml` e `compose.local.yaml`.
