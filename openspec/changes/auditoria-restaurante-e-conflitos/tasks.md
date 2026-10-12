# Tasks

Pré-requisito: `deploy-viavel` aplicado (Flyway, `V1`, unicidade de refeição). Preferencialmente também `endurecer-dominio-e-persistencia` (design, Risks).

## 1. Auditoria de datas no domínio

- [x] 1.1 Adicionar `createdAt`/`updatedAt` (`Instant`) a `CostCenter`, `Client`, `Employee`, `Project`, `Accommodation`, `Meal` e `User` via valor `AuditStamp` e base `Audited` (design D1, ajuste), validados como não nulos e `updatedAt >= createdAt`, com `touch(Instant)` que só altera `updatedAt`. Verificar por testes unitários de domínio: nulo rejeitado, `updatedAt` anterior rejeitado, `touch` não altera `createdAt`.
- [x] 1.2 Injetar `Clock` nos casos de uso desses recursos via `UseCaseConfig`. Criação usa `now` nos dois campos; alteração preserva `createdAt` lido e chama `touch(now)`. Verificar por testes de caso de uso com `Clock.fixed`: criação com datas iguais, alteração avança só `updatedAt`, alteração rejeitada mantém o valor anterior.
- [x] 1.3 Criar a parte de auditoria do `V3__auditoria_e_restaurante.sql` (passo 1 do Migration Plan) e as colunas nas entidades JPA. Os mappers de entidade copiam as datas sem gerá-las. Verificar por teste de integração que o boot aplica o `V3` em banco vazio e que um registro gravado antes do `V3` (inserido via SQL no teste) é lido com datas preenchidas.
- [x] 1.4 Expor `createdAt`/`updatedAt` nos DTOs de resposta e de resumo. Ignorar os campos se vierem na requisição. Verificar por teste de API que POST e PUT com datas no corpo devolvem as datas do servidor, e que listagem e detalhe devolvem os mesmos valores.
- [x] 1.5 Garantir que escritas indiretas na obra (vínculo de hospedagem, recálculo de total do `endurecer-dominio-e-persistencia` se já aplicado) não alteram `updatedAt`. Verificar por teste que criar, alterar e excluir refeição e hospedagem mantém o `updatedAt` da obra.

## 2. Ordenação da listagem de obras

- [x] 2.1 Adicionar `Sort` opcional ao `PageRequest` do `core` e `InvalidPageException.invalidSort` (`page.invalid.sort`). Verificar por testes unitários: formato `campo,direção`, direção padrão `asc`, direção inválida rejeitada.
- [x] 2.2 Aceitar `sort` em `GET /api/projects`, validar contra a lista `updatedAt`, `createdAt`, `os`, `startDate` e traduzir em `SpringPageSupport` com desempate por `id`. Verificar por teste de API: `sort=updatedAt,desc&size=10` em ordem decrescente; `sort=totalPrice` e `sort=updatedAt,up` respondem 400 `page.invalid.sort`; percorrer páginas com `updatedAt` empatados não repete nem pula obras; requisição sem `sort` continua 200.
- [x] 2.3 Documentar o parâmetro `sort` na OpenAPI de `GET /api/projects`, com a lista de campos. Verificar em `/v3/api-docs`.

## 3. Conflitos com 409

- [x] 3.1 Criar `ConflictException` no `core/exception` com `errorCode` e mapear para 409 no `GlobalExceptionHandler`. Verificar por teste do handler.
- [x] 3.2 Adicionar às portas as consultas de existência (OS, e-mails, CPF, excluindo o próprio id) e de dependentes, e chamá-las nos casos de uso antes de gravar e de excluir. Verificar por teste de API cada código de `data-conflict-responses`: duplicidade na criação e na alteração, manter o próprio valor aceito, e-mail com caixa diferente, e cada exclusão bloqueada com 409 e dado preservado.
- [x] 3.3 Traduzir `DataIntegrityViolationException` para 409 no handler, com tabela fixa nome da restrição → `errorCode` e `data.integrity_violation` como padrão, sem texto do banco na mensagem. Verificar por teste que força cada violação contornando o caso de uso (SQL direto) e confere o código.
- [x] 3.4 Ajustar `deploy-viavel`: marcar a tarefa 3.3 como atendida por este change e registrar que a duplicidade de refeição responde 409 `meal.duplicate.type`. Verificar com `openspec validate deploy-viavel --strict`.

## 4. Troca de centro de custo do colaborador

- [x] 4.1 Corrigir `EmployeeEntityMapper.apply` para trocar a referência para a `CostCenterEntity` de destino, sem chamar `CostCenterEntityMapper.apply`. Verificar por teste de API: colaborador movido de A para B aparece em `by-cost-center/B` e não em `by-cost-center/A`; nome e CNPJ de A e B inalterados; `costCenterId` inexistente responde 404 e mantém o colaborador em A.

## 5. Restaurante

- [x] 5.1 Criar o domínio `Restaurant` (nome 2–100, telefone opcional 10–11 dígitos, endereço opcional, obra obrigatória, datas de auditoria) e `InvalidRestaurantException`. Verificar por testes unitários com os códigos `restaurant.invalid.name`, `restaurant.invalid.phone` e `restaurant.invalid.project`.
- [x] 5.2 Criar a parte de restaurante do `V3` (passos 2 a 6 do Migration Plan), `RestaurantEntity` com cascade de endereço, `RestaurantJpaRepository` e o adaptador. Verificar por teste de integração de migração: banco com duas refeições de mesmo nome na mesma obra resulta em um restaurante, referenciado pelas duas, com endereço próprio.
- [x] 5.3 Criar `RestaurantRepository`, `RestaurantUseCase`, DTOs, mapper e `RestaurantController` (`/api/restaurants` com CRUD, paginação e `/by-project/{projectId}`), registrados em `UseCaseConfig` e cobertos pelas regras de papel existentes. Verificar por teste de API os cenários de `restaurant-management`: criação com obra, sem obra (400), obra inexistente (404), listagem por obra, troca de obra com e sem refeições (409 `restaurant.in_use` / aceita), exclusão com refeições (409).
- [x] 5.4 Trocar a refeição para `restaurantId`: `Meal` referencia `Restaurant`; `MealRequestDTO` perde `restaurantName`, `address` e `projectId`; o adaptador grava `meal.project_id` a partir do restaurante. Verificar por teste de API: refeição herda a obra do restaurante; sem `restaurantId` responde 400 `meal.invalid.restaurant`; restaurante inexistente responde 404; segundo almoço por outro restaurante da mesma obra responde 409 `meal.duplicate.type`.
- [x] 5.5 Alterar `ProjectRepositoryAdapter.assignMeals` para não reescrever `meal.project_id`, e `ProjectUseCase` para só validar `lunchId`/`dinnerId` (existência, tipo, mesma obra). Verificar por teste de API: refeição de outra obra responde 400 `project.invalid.meal` e não se move; PUT sem `lunchId`/`dinnerId` mantém almoço e janta na leitura.
- [x] 5.6 Adicionar ao `ProjectResponseDTO` a lista resumida de restaurantes da obra, sem mudar os campos existentes. Verificar por teste de API.
- [x] 5.7 Documentar na OpenAPI do `PUT /api/projects/{id}` a substituição completa de `employeeIds`/`accommodationIds` e o papel apenas validador de `lunchId`/`dinnerId`. Registrar no README o **BREAKING** de `MealRequestDTO`. Verificar em `/v3/api-docs` e no README.
- [ ] 5.8 Atualizar `app-adm-obras-erd.puml` com `restaurant`, `meal.restaurant_id` e colunas de auditoria, e regerar o PDF com `./mvnw -Pdiagrams`. Verificar que o diagrama bate com o `V3`.

  > Pendente: `app-adm-obras-erd.puml` atualizado e com sintaxe válida (PlantUML `-syntax`: 15 entidades), mas o PDF não foi regenerado. O perfil `diagrams` do `pom.xml` já falhava antes deste change (`ClassNotFoundException: net.sourceforge.plantuml.Run` no `exec:java`), gera SVG e não PDF, e o ambiente não tem Graphviz.

## 6. Seed de dados fictícios (removido)

- [x] 6.1 Remover do backend o seed implementado neste change (`DevSeedRunner`, `SteppingClock`, `app.seed.enabled`/`APP_SEED_ENABLED` e testes). Os dados fictícios passam a ser cadastrados pelo frontend via API (`npm run seed`). O gerador de CPF/CNPJ fica só nos testes (`support/FakeDocuments`). Verificar com `./mvnw test`.

## 7. Verificação de conjunto

- [x] 7.1 Rodar `./mvnw test` e confirmar suíte completa verde.
- [x] 7.2 Subir em `local` com `APP_BOOTSTRAP_ENABLED=true`, cadastrar dados fictícios pela API e conferir via curl, com Bearer: `GET /api/projects?sort=updatedAt,desc&size=10` retorna 10 obras ordenadas com `createdAt`/`updatedAt`; `GET /api/restaurants/by-project/{id}` retorna restaurantes; exclusão de centro de custo com colaboradores responde 409.
- [x] 7.3 Validar com `openspec validate auditoria-restaurante-e-conflitos --strict`.

## Workflow follow-up

- Arquivar depois de `deploy-viavel` e `endurecer-dominio-e-persistencia`.
- O frontend (`App-adm-obras-front`) consome este contrato: dashboard por `sort=updatedAt,desc`, telas de restaurante e mensagens por `errorCode`.
