## Why

O frontend administrativo precisa de um dashboard com as obras alteradas mais recentemente e de telas de cadastro para todos os recursos. Hoje a API não sustenta isso:

1. **Não há data de criação nem de alteração.** Nenhuma tabela de negócio tem coluna de auditoria (`V1__baseline_schema.sql`), e a listagem paginada não aceita ordenação (`SpringPageSupport.toPageable`). Não há como saber quais obras mudaram por último.
2. **Restaurante não existe como entidade.** É apenas o texto `restaurantName` dentro de cada refeição. O mesmo restaurante é redigitado a cada refeição e não pode ser cadastrado nem consultado. A regra de negócio é que todo restaurante atende uma obra, e hoje nada a representa.
3. **Conflitos de dados respondem 500.** OS, e-mail e CPF duplicados, e exclusões bloqueadas por vínculo (centro de custo com colaboradores, obra com refeições etc.), chegam como `DataIntegrityViolationException` sem tratamento. O frontend recebe 500 sem `errorCode` e não consegue explicar o motivo ao usuário.
4. **Trocar o centro de custo de um colaborador corrompe dados.** `EmployeeEntityMapper.apply` copia nome e CNPJ do centro novo sobre o registro do centro atual, em vez de apontar o colaborador para o centro novo. O centro antigo é renomeado em silêncio e o colaborador não muda.
5. **Ambiente local sem dados.** Os perfis `local` e `test` usam H2 em memória; a cada reinício só existe o administrador. Desenvolver e demonstrar o frontend exige recadastrar tudo à mão.

## What Changes

### Auditoria de datas (desde o domínio)

- Os domínios `Project`, `Employee`, `CostCenter`, `Client`, `Accommodation`, `Meal`, `Restaurant` e `User` passam a ter `createdAt` e `updatedAt`, definidos pelos casos de uso a partir de um `Clock` injetado, sem depender de recurso do JPA.
- `createdAt` é imutável após a criação. `updatedAt` muda a cada alteração gravada pela API do próprio recurso.
- Os DTOs de resposta passam a expor `createdAt` e `updatedAt`.
- Migração `V3` adiciona as colunas. Linhas já existentes recebem o instante da migração.

### Ordenação da listagem de obras

- `GET /api/projects` passa a aceitar `sort` (ex.: `sort=updatedAt,desc`), restrito a uma lista fechada de campos. Campo fora da lista responde 400.
- A ordenação entra no `PageRequest` do `core`, sem expor tipos do Spring ao domínio.

### Restaurante

- Nova entidade `Restaurant`, com CRUD em `/api/restaurants` e listagem por obra.
- Todo restaurante pertence a exatamente uma obra. Criar restaurante sem obra é rejeitado.
- **BREAKING** (API): a refeição passa a referenciar um restaurante (`restaurantId`) em vez de carregar `restaurantName`, `address` e `projectId`. A obra da refeição passa a ser a obra do restaurante.
- A unicidade de uma refeição de cada tipo por obra (`meal-assignment-uniqueness`, change `deploy-viavel`) continua valendo, agora sobre a obra derivada do restaurante.
- `V3` migra os dados existentes: cria um restaurante por par (obra, nome) a partir das refeições e liga cada refeição ao seu restaurante.

### Conflitos de dados respondem 409

- Duplicidade de OS, e-mail de cliente, e-mail e CPF de colaborador, e e-mail de usuário (inclusive na alteração) é verificada pelo caso de uso antes de gravar e responde **409** com `errorCode` específico.
- Exclusão de registro que ainda tem dependentes responde **409** com `errorCode` que nomeia o vínculo, sem apagar nada.
- Violação de integridade que escape das verificações da aplicação responde 409 com código genérico, e não 500.
- **Substitui** a tarefa 3.3 de `deploy-viavel`, que traduzia a violação de integridade para 400. Duplicidade de refeição por tipo passa a responder 409.

### Troca de centro de custo do colaborador

- Alterar `costCenterId` de um colaborador passa a mover o colaborador para o centro informado. Nenhum centro de custo é modificado por essa operação.

### Contrato do PUT de obra (documentação)

- O `PUT /api/projects/{id}` é substituição completa: `employeeIds`, `accommodationIds`, `lunchId` e `dinnerId` omitidos desfazem os vínculos. O comportamento não muda; a documentação OpenAPI do endpoint passa a dizê-lo.

### Não-objetivos

- Dados fictícios de desenvolvimento no backend. O seed fica no frontend (`npm run seed`), cadastrando pela API.

- Ordenação nas listagens dos demais recursos. Só a de obras é necessária agora; a estrutura no `core` permite estender.
- Histórico de alterações (quem alterou, valor anterior).
- Propagar a data de alteração da obra quando mudam seus restaurantes, refeições ou hospedagens.
- Mudar a semântica do PUT de obra para alteração parcial.

## Capabilities

### New Capabilities

- `record-audit-timestamps`: todo registro de negócio informa quando foi criado e quando foi alterado pela última vez, com valores definidos pelo servidor.
- `project-listing-order`: a listagem de obras pode ser ordenada por campos permitidos, entre eles a data de alteração.
- `restaurant-management`: cadastro de restaurantes vinculados obrigatoriamente a uma obra, e refeições que referenciam um restaurante.
- `data-conflict-responses`: duplicidades e exclusões bloqueadas por vínculo respondem 409 com código de erro específico, sem alterar dados.
- `employee-cost-center-assignment`: mover um colaborador de centro de custo não altera nenhum centro de custo.

### Modified Capabilities

(nenhuma; `openspec/specs/` ainda está vazio. `meal-assignment-uniqueness`, do change `deploy-viavel`, ainda não foi arquivada. Este change mantém seus requisitos e só troca o código de resposta da duplicidade de 400 para 409; o ajuste é registrado no design e na tarefa correspondente.)

## Impact

**Pré-requisitos**
- `deploy-viavel` aplicado (Flyway, `V1`, unicidade de refeição). Este change adiciona `V3`.
- Conflita com `endurecer-dominio-e-persistencia` em `MealUseCase`, `AccommodationUseCase` e `ProjectRepositoryAdapter`. A ordem de aplicação precisa ser definida (ver design).

**Código**
- `core/domain`: `Project`, `Employee`, `CostCenter`, `Client`, `Accommodation`, `Meal`, `User` (datas); novo `Restaurant`; `Meal` passa a referenciar `Restaurant`.
- `core/repository`: `PageRequest` com ordenação; novo `RestaurantRepository`; consultas de existência para duplicidade e dependentes.
- `core/exception`: nova exceção de conflito.
- `application`: casos de uso com `Clock`, novo `RestaurantUseCase`, DTOs e mappers.
- `infra`: entidades JPA, adaptadores, `RestaurantController`, `GlobalExceptionHandler` (409), `EmployeeEntityMapper` (correção).
- `src/main/resources/db/migration/V3__auditoria_e_restaurante.sql`.

**API**
- Novos campos `createdAt`/`updatedAt` em todas as respostas de negócio.
- `GET /api/projects?sort=`.
- Novo `/api/restaurants`.
- **BREAKING**: `MealRequestDTO` troca `restaurantName`, `address` e `projectId` por `restaurantId`; `MealResponseDTO` passa a trazer o restaurante.
- 409 em duplicidades e exclusões bloqueadas, onde hoje há 500.

**Documentação**
- `app-adm-obras-erd.puml` / `.pdf` regerados com `restaurant` e as colunas de auditoria.
