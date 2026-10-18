## Why

A API não tem como medir o próprio desempenho, e a leitura de código já mostra custo de consulta que cresce com o tamanho da página. Também não há registro de quem alterou um cadastro nem de quem entrou no sistema e quando. Hoje só existem `createdAt` e `updatedAt`, sem autor.

Problemas observados no código:

1. **Uma requisição, uma thread de plataforma.** O Tomcat atende com o pool padrão de threads de plataforma. A aplicação roda em Java 25, que já suporta virtual threads sem o problema de *pinning* em `synchronized` (JEP 491), mas o recurso não está ligado.
2. **N+1 nas listagens.** Exemplos verificados:
   - `ProjectRepositoryAdapter.toDomain` executa `mealJpaRepository.findByProjectId` uma vez por obra da página.
   - `ProjectEntity.employees` e `AccommodationEntity.employees`/`periods` são `EAGER`.
   - `ProjectEntity.restaurants` e `accommodations` são carregados um a um pelo mapper.
   - `ClientRepositoryAdapter.toDomain` consulta as obras de cada cliente.
   - `UserEntity.roles` é `@ElementCollection(EAGER)`.
   - `MealEntity.restaurant` é `EAGER` e arrasta restaurante → obra → colaboradores.
3. **N+1 na escrita.** `ProjectRepositoryAdapter.applyRelations` e `AccommodationRepositoryAdapter` fazem um `findById` por colaborador e por hospedagem vinculados.
4. **Índices sem evidência.** O `V1` indexa as FKs. Não há medição dos filtros e ordenações reais (busca `q`, `sort`, limpeza de refresh token por `expires_at`) contra PostgreSQL com volume.
5. **Sem auditoria de autoria.** Não há registro de quem criou, alterou ou excluiu um registro, nem de quem fez login.

## What Changes

### Medição de desempenho

- Teste automatizado de contagem de consultas SQL por endpoint de listagem e de detalhe, na suíte do `mvnw test`. A suíte passa a falhar quando o número de consultas cresce com o tamanho da página.
- Massa de dados criada por script que chama os endpoints da API, com as mesmas validações do uso real. O volume é escolhido por cenário nomeado (`pequeno`, `medio`, `grande`), com valores nominais e editáveis. Isso substitui a dependência do volume real de produção.
- Roteiro de carga k6, executado via Docker contra PostgreSQL, com uma execução com virtual threads ligadas e outra com elas desligadas. Pode rodar contra a aplicação local (perfil `local`) ou contra o ambiente isolado `perf/`. O resultado (latência p50/p95/p99, vazão, taxa de erro) vira relatório versionado, base para as propostas de melhoria.

### Perfil local com PostgreSQL em contêiner

- O perfil `local` deixa o H2 e passa a usar PostgreSQL em contêiner Docker, iniciado pela própria aplicação na subida. Os dados persistem entre reinícios.
- Rodar a aplicação localmente passa a exigir Docker. O console do H2 sai do perfil `local`.
- O perfil `test` continua em H2: `mvnw test` não exige Docker.
- Nenhum ganho de desempenho é afirmado antes da medição. Os números do relatório são os medidos.

### Virtual threads

- Atendimento HTTP e tarefas agendadas passam a rodar em virtual threads (`spring.threads.virtual.enabled`), ligado por padrão em todos os perfis e desligável por variável de ambiente para comparação.
- O tamanho do pool de conexões passa a ser configurável por variável de ambiente, porque com virtual threads o pool vira o limite de concorrência efetivo.

### Persistência: N+1 e índices

- Associações `EAGER` passam a `LAZY`, com carga em lote (`hibernate.default_batch_fetch_size`) dentro da transação do adaptador.
- As refeições da página de obras e as obras da página de clientes passam a ser carregadas em uma consulta por página.
- Os vínculos gravados por obra e por hospedagem passam a ser resolvidos em uma consulta (`findAllById`), sem um `findById` por item.
- Nova migração com os índices que a análise de plano (`EXPLAIN ANALYZE` em PostgreSQL com massa) mostrar necessários, e com os índices das tabelas de auditoria. Índice sem ganho medido não entra.
- **Absorve** a parte de leitura de `endurecer-dominio-e-persistencia`: o grupo 2 de tarefas e a spec `project-read-performance` daquele change passam a ser cobertos por este, e lá são marcados como substituídos. O grupo 1 (total do projeto) continua lá.

### Auditoria de alterações

- Cada criação, alteração e exclusão de cadastro de negócio gera um evento: usuário autor, tipo de registro, id do registro, ação e instante. Não guarda os valores alterados (decisão do usuário, 2026-10-18).
- O evento é gravado na mesma transação da alteração. Se a alteração não for confirmada, o evento também não é.
- Novo `GET /api/audit/changes`, paginado e filtrável por tipo, registro, autor e período. Somente ADMIN.

### Auditoria de acesso

- Cada login com sucesso gera um registro: usuário, instante, IP e user-agent. Falhas, refresh e logout não são registrados (decisão do usuário, 2026-10-18).
- Novo `GET /api/audit/logins`, paginado e filtrável por usuário e período. Somente ADMIN.

### Não-objetivos

- Histórico de valores (antes/depois) dos campos alterados.
- Registro de tentativas de login falhas, refresh e logout.
- Expurgo automático dos registros de auditoria. A retenção fica para decisão posterior.
- Cache de aplicação, réplica de leitura ou mudança de infraestrutura de hospedagem.
- Rastreamento distribuído e métricas em tempo real (Actuator/Micrometer).

## Capabilities

### New Capabilities

- `virtual-thread-execution`: atendimento de requisições e tarefas agendadas em virtual threads, com chave de liga/desliga e pool de conexões configurável.
- `persistence-query-efficiency`: custo de consulta das leituras e escritas independente do tamanho da página e das coleções; índices sustentados por medição. Substitui `project-read-performance` de `endurecer-dominio-e-persistencia`.
- `performance-testing`: contagem automatizada de consultas por endpoint e teste de carga reproduzível com relatório comparativo.
- `change-audit-log`: registro e consulta de quem criou, alterou ou excluiu cada registro de negócio, e quando.
- `login-audit-log`: registro e consulta de quem fez login, quando, de qual IP e com qual user-agent.

### Modified Capabilities

Nenhuma. `openspec/specs/` está vazio; as specs dos changes ainda não arquivados não são base.

## Impact

- **Configuração:** `application.properties` (virtual threads, batch fetch, pool Hikari), `application-test.properties`, `application-local.properties` (PostgreSQL via Docker Compose do Spring Boot, novo `compose.local.yaml`).
- **Ambiente de desenvolvimento:** perfil `local` exige Docker. README atualizado.
- **Entidades e adaptadores:** `infra/entities/*` (fetch), `infra/adapters/repository/*` (carga em lote), `infra/repositories/*` (consultas `...In`).
- **Auditoria:** novas portas em `core`, registro de eventos em `infra` (listener do Hibernate), `AuthUseCase.login` e `AuthController.login` (IP e user-agent), novo controller `/api/audit`, regra de segurança ADMIN.
- **Banco:** nova migração `V9` (tabelas `change_audit` e `login_audit`, índices medidos). Válida em PostgreSQL e em H2 com `MODE=PostgreSQL`.
- **Teste e carga:** novos testes de contagem de consultas; diretório `perf/` com `compose` (PostgreSQL + app + k6), cenários de volume (`scenarios.json`), massa criada pela API (`seed.js`) e roteiro k6; relatório em `perf/RESULTADOS.md`.
- **API:** dois endpoints novos de leitura. Nenhum contrato existente muda.
- **Dependências:** nenhuma no `pom.xml`. O k6 roda por imagem Docker.
- **Dados pessoais:** IP e user-agent passam a ser armazenados (LGPD). A retenção fica em aberto.
