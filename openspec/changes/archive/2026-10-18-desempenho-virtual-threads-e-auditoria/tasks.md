# Tasks

## 1. Contagem de consultas (linha de base)

- [x] 1.1 Criar `support/QueryCounter` (`StatementInspector` que conta os SELECT durante a chamada; ver design D2) e verificar com um teste simples que uma leitura por id conta ao menos 1 consulta
- [x] 1.2 Criar `QueryCountIntegrationTest` com o inspetor e banco H2 próprio só na classe, cobrindo as 8 listagens (página 5 × página 50, com 50+ registros criados por `AdminApi`), a listagem com `q`, e o detalhe de obra, hospedagem, refeição e cliente (2 × 30 vínculos); a mensagem de falha traz endpoint e contagens. Verificar que o teste roda e registrar quais casos falham hoje (esperado: obras, clientes, hospedagens, refeições, usuários)
- [x] 1.3 Adicionar ao mesmo teste a gravação de obra e de hospedagem com 2 × 30 colaboradores e verificar que hoje falha por crescer com a lista

## 2. Perfil local com PostgreSQL

- [x] 2.1 Criar `compose.local.yaml` (serviço `postgres` com versão fixa e volume nomeado) e alterar `application-local.properties` (Docker Compose do Boot ligado, `start-only`, sem H2, sem console do H2, sem `baseline-on-migrate`). Verificar com `./mvnw spring-boot:run` que o contêiner sobe, o Flyway migra do `V1` e o login do admin responde 200
- [x] 2.2 Reiniciar a aplicação local e verificar que os dados criados continuam e que o bootstrap não duplica o admin; verificar que `BootstrapProfilePropertiesTest`, `OpenApiProfilePropertiesTest` e `SecurityProfilePropertiesTest` passam e que `./mvnw test` roda sem Docker (perfil `test` em H2)
- [x] 2.3 Atualizar o `README.md`: perfil `local` exige Docker, como zerar a base (`docker compose -f compose.local.yaml down -v`), e remoção do console do H2 no `local`

## 3. Ambiente de carga, massa pela API e medição inicial

- [x] 3.1 Criar `perf/compose.yaml` (PostgreSQL com a mesma versão de `compose.local.yaml`, `grafana/k6` com versão fixa, app pelo `Dockerfile` em perfil `prod`) e `perf/admin.sql` (ADMIN de carga com hash BCrypt fixo); verificar que o app sobe, o Flyway migra e o login do ADMIN de carga responde 200
- [x] 3.2 Criar `perf/scenarios.json` (`pequeno`, `medio`, `grande`, valores nominais do design D7) e `perf/seed.js` (login ADMIN, criação pelos endpoints na ordem de dependência, CPF/CNPJ válidos, parada no primeiro cadastro rejeitado com endpoint e resposta, cenário inexistente falha antes de criar). Verificar rodando `pequeno` contra a app local e conferindo o `totalElements` de cada listagem
- [x] 3.3 Criar `perf/script.js`: login uma vez por VU, listagens (tamanho 20), detalhes, busca `q`, criação e exclusão de centro de custo com CNPJ válido, `VUS`/`DURATION` parametrizáveis, grupos de métricas por tipo de endpoint; verificar com execução curta (1 VU, 30 s) sem erros contra a app local
- [x] 3.4 Criar `perf/run.sh --target local|perf --scenario <nome> --threads on|off` (design D7; no `local`, massa só com base vazia e k6 por `host.docker.internal`); verificar uma execução em cada alvo, com JSON nomeado por data, alvo, cenário e modo
- [x] 3.5 Criar `perf/RESULTADOS.md` com o modelo de entrada (data, commit, alvo, cenário, VUs, duração, pool, CPU/RAM, p50/p95/p99, vazão e taxa de erro por grupo; volumes declarados como nominais) e registrar a linha de base no alvo `perf/`, antes das correções; verificar que todos os valores vêm do JSON gerado
- [x] 3.6 Documentar no `README.md` como criar a massa, executar o teste de carga nos dois alvos e onde ficam os resultados

## 4. Persistência: N+1

- [x] 4.1 Adicionar `spring.jpa.properties.hibernate.default_batch_fetch_size=100` e trocar todas as associações `EAGER` das entidades por `LAZY` (inclusive `UserEntity.roles`); remover o `@BatchSize` local de `MealEntity.adjustments`. Verificar que a suíte existente continua verde
- [x] 4.2 Revisar todos os métodos públicos dos adaptadores que mapeiam entidade → domínio e garantir `@Transactional` (ou `readOnly = true`); verificar que os testes de integração de listagem e detalhe passam sem `LazyInitializationException`
- [x] 4.3 Adicionar `MealJpaRepository.findByProjectIdIn` e carregar as refeições da página de obras em uma consulta, agrupadas por obra, em `findAll` paginado, `findAll()` e nas listas por cliente, centro de custo e colaborador; verificar que os casos de obras do `QueryCountIntegrationTest` passam
- [x] 4.4 Adicionar `ProjectJpaRepository.findByClientIdIn` e carregar as obras da página de clientes em uma consulta; verificar que os casos de clientes passam
- [x] 4.5 Trocar os `findById` por item de `ProjectRepositoryAdapter.applyRelations` e `AccommodationRepositoryAdapter` por `findAllById` (inclui `findAllById` nas portas `EmployeeRepository`/`AccommodationRepository` e no `EntityLookup.all` dos casos de uso, que também buscavam um a um; obras de colaborador também por `IN`); verificar que o caso de gravação do item 1.3 passa
- [x] 4.6 Verificar que todos os casos do `QueryCountIntegrationTest` passam e que listagem e detalhe da mesma obra devolvem o mesmo conteúdo (teste comparando as duas respostas)

## 5. Virtual threads

- [x] 5.1 Adicionar `spring.threads.virtual.enabled=${APP_VIRTUAL_THREADS:true}` e `spring.datasource.hikari.maximum-pool-size=${DB_POOL_MAX_SIZE:10}` em `application.properties`, e as variáveis no `.env.example`; verificar que a aplicação sobe nos perfis `local` e `test`
- [x] 5.2 Criar controller só de teste que devolve `Thread.currentThread().isVirtual()` e dois testes: padrão → `true`; `APP_VIRTUAL_THREADS=false` → `false`
- [x] 5.3 Verificar que `RefreshTokenCleanupTest` e os testes de refresh concorrente (janela de tolerância; novo teste com 4 refresh simultâneos do mesmo token) passam com virtual threads ligadas

## 6. Schema de auditoria

- [x] 6.1 Criar `V9__auditoria_de_alteracoes_e_login.sql` com `change_audit` e `login_audit` e seus índices (design D8); verificar com `SchemaMigrationIntegrationTest` em H2 e subindo o perfil `local` (PostgreSQL) que a migração aplica sem erro

## 7. Auditoria de alterações

- [x] 7.1 Criar o mapa classe de entidade → tipo de registro (10 tipos) e o resolvedor de autor (`SecurityContextHolder` → id; nome e e-mail consultados uma vez por transação; `SYSTEM` sem autenticação); verificar por teste unitário o mapa e o caso `SYSTEM`
- [x] 7.2 Registrar os listeners `PostInsert`/`PostUpdate`/`PostDelete` no `EventListenerRegistry`, gravando por `session.doWork` com instante do `Clock`; verificar por teste de integração os cenários de criar cliente, alterar obra e excluir restaurante
- [x] 7.3 Verificar por teste de integração: renovação de hospedagem gera `ACCOMMODATION_PERIOD`/`CREATE`; exclusão de refeição com ajustes gera `DELETE` de cada ajuste (cascata via JPA, não só do banco)
- [x] 7.4 Verificar por teste de integração: alteração rejeitada por 409 e por 400 não grava evento; requisição sem gravação não grava evento; bootstrap gera `USER`/`CREATE` com autor `SYSTEM`
- [x] 7.5 Verificar por teste que nome do autor alterado depois não muda o evento, e que o evento de troca de senha não contém senha nem hash (a tabela não tem coluna de valores)

## 8. Auditoria de login

- [x] 8.1 Criar `LoginRecord` (classe, sem `record`), porta `LoginAuditRepository` em `core/repository` e adaptador JPA; verificar por teste do adaptador que grava e lê
- [x] 8.2 Alterar `AuthUseCase.login` para receber `LoginOrigin` (IP, user-agent) e gravar o registro após autenticar e antes de emitir os tokens; ajustar `AuthController.login` (remote addr, `User-Agent` truncado em 512). Verificar com `AuthUseCaseTest`: sucesso grava, falha não grava, falha na gravação não emite token
- [x] 8.3 Verificar por teste de integração: login sem `User-Agent` funciona com campo vazio; refresh e logout não geram registro
- [x] 8.4 Documentar `SERVER_FORWARD_HEADERS_STRATEGY` no `.env.example` e no README (ligar só atrás de proxy confiável)

## 9. Consulta da auditoria

- [x] 9.1 Criar portas de leitura paginada com filtro, `AuditQueryUseCase` e validação de filtros (tipo desconhecido, `from > to` → 400 com `errorCode`); verificar por teste unitário do caso de uso
- [x] 9.2 Criar `AuditController` com `GET /api/audit/changes` e `GET /api/audit/logins` (ordem `occurred_at desc, id desc`, paginação existente); verificar por teste de integração os filtros por registro, autor, usuário e período
- [x] 9.3 Adicionar `.requestMatchers("/api/audit/**").hasRole("ADMIN")` antes da regra de `GET /api/**` em `SecurityConfig`; verificar por teste: OPERADOR → 403, sem token → 401, ADMIN → 200
- [x] 9.4 Verificar que os dois endpoints aparecem no `/v3/api-docs` (`OpenApiDocumentationIntegrationTest`)

## 10. Índices e comparação final

- [x] 10.1 Com a massa do cenário `grande` em PostgreSQL (perfil `local` ou `perf/`), capturar as consultas do roteiro e rodar `EXPLAIN (ANALYZE, BUFFERS)` nos candidatos do design D4; registrar planos antes e depois em `perf/RESULTADOS.md`
- [x] 10.2 (Resultado: nenhuma migração. O trigram só ajuda com constantes literais no SQL, e o caminho real usa plano genérico; por decisão do usuário em 2026-10-18, a proposta ficou registrada em `perf/RESULTADOS.md` para um change próprio.) Criar migração com os índices que melhoraram o plano (trigram, se houver, em `db/vendor/postgresql/` com `spring.flyway.locations` por fornecedor); verificar migração em H2 e PostgreSQL. Se nenhum índice melhorar o plano, registrar isso no relatório e não criar migração
- [x] 10.3 Rodar `perf/run.sh --target perf --threads off` e `--threads on` com o mesmo cenário e os mesmos parâmetros, com JFR para `jdk.VirtualThreadPinned` no modo `on`; registrar as duas execuções e os eventos de pinning em `perf/RESULTADOS.md`
- [x] 10.4 Escrever em `perf/RESULTADOS.md` a seção de propostas de melhoria: cada uma com o número medido que a sustenta, ou marcada como hipótese

## 11. Sobreposição e fechamento

- [x] 11.1 Em `endurecer-dominio-e-persistencia`, marcar o grupo 2 de `tasks.md` como substituído por este change e remover `specs/project-read-performance`; verificar com `openspec validate endurecer-dominio-e-persistencia`
- [x] 11.2 Rodar `./mvnw test` e confirmar a suíte completa verde
- [x] 11.3 Rodar `openspec validate desempenho-virtual-threads-e-auditoria --strict` e confirmar que passa
