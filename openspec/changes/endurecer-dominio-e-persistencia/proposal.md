## Why

O empilhamento do projeto está sólido: `core/` e `application/` não têm um único import do Spring. Ainda assim, duas costuras estão quebradas.

1. **Totais desatualizados.** `project.total_price` é estado derivado: calculado no domínio a partir das refeições e acomodações, e também gravado como coluna. Só `PUT /api/projects/{id}` o recalcula. `PUT /api/meals/{id}`, `PUT /api/accommodations/{id}` e `DELETE /api/accommodations/{id}` alteram os componentes e deixam o total do projeto parado. A API chega a devolver um total que contradiz as próprias linhas que o compõem. É um bug de integridade visível ao usuário final, não uma questão de estilo.
2. **Custo de consulta linear na página.** `ProjectRepositoryAdapter.toDomain` dispara duas queries por projeto para resolver almoço e janta, somadas à coleção `EAGER` de funcionários. `GET /api/projects?size=20` custa cerca de 62 consultas para renderizar 20 linhas.

A gestão de schema (Flyway) e a unicidade de refeição no banco, antes parte deste change, foram movidas para `deploy-viavel`, que destrava o primeiro deploy. A limpeza das portas de repositório foi descartada.

## What Changes

### Consistência do total do projeto

- Toda mutação de refeição ou acomodação (create, update, delete, e também mover uma refeição entre projetos) passa a recalcular e persistir `project.total_price` **na mesma transação** da mutação que o invalidou. A coluna passa a ser a fonte da verdade, e o cálculo do domínio é a única origem do valor.
- O caminho de escrita do total passa a existir no adaptador de persistência, orquestrado pela camada de aplicação, o que mantém `core` e `application` livres de framework.

### Custo de consulta

- A listagem de projetos passa a carregar refeições e funcionários em lote por página, sem as duas queries por linha. O número de consultas de `GET /api/projects` deixa de crescer com o tamanho da página.
- As coleções `EAGER` de projeto passam a `LAZY` com `@BatchSize`, dentro da transação do adaptador.

### Hipóteses registradas

- **O total da acomodação continua sendo valor informado, não derivado.** `Accommodation.totalPrice` é cotado pelo anfitrião e só validado como não negativo; `Meal.totalPrice` é derivado de `price x quantity`. O change **não** unifica as duas regras, porque isso mudaria o contrato da API. Em vez disso, a distinção fica explícita e coberta por teste, para que a soma em `Project.calculateTotalPrice` não dependa de as duas terem a mesma origem.

### Não-objetivos

- **Autenticação.** HTTP Basic sem token, com `csrf.disable()`, é uma escolha defensável para API interna atrás do frontend. Trocar por JWT ou sessão no servidor é decisão de produto e de superfície de segurança, para um change próprio.
- **Novos filtros de consulta na API.** Nenhuma query por período, tipo ou centro de custo é exposta como endpoint.
- **Gestão de schema e unicidade de refeição**, agora em `deploy-viavel`.
- **Remoção de métodos sem uso das portas de repositório.** Descartada.

### Dependência

- Este change depende de `deploy-viavel`. As novas consultas e as alterações de mapeamento (`LAZY`) precisam ser validadas contra o schema versionado pelo `V1` e com `ddl-auto=none`.

## Capabilities

### New Capabilities

- `project-cost-totalization`: o total de custo de um projeto reflete, de forma consistente e observável, todas as suas refeições e acomodações em todos os caminhos de mutação, e é persistido como fonte da verdade.
- `project-read-performance`: a leitura de projetos, em especial a listagem paginada, executa um número de consultas que não cresce com o tamanho da página.

### Modified Capabilities

(nenhuma; o projeto ainda não possui specs.)

## Impact

**Domínio**
- `core/domain/Accommodation.java` / `Meal.java`: contrato explícito da origem do total.

**Portas**
- `core/repository/ProjectRepository.java`: nova entrada para recálculo do total.

**Aplicação**
- Novo `application/usecase/ProjectTotalRefresher`, injetado em `MealUseCase` e `AccommodationUseCase`.

**Infra**
- `infra/adapters/repository/ProjectRepositoryAdapter.java`: implementação do recálculo; refeições resolvidas em lote em vez de duas queries por linha.
- `infra/repositories/MealJpaRepository.java`: consulta por lista de projetos.
- `infra/entities/ProjectEntity.java`, `AccommodationEntity.java`: `LAZY` + `@BatchSize` nas coleções.
- `infra/config/UseCaseConfig.java`: registro do novo colaborador.

**API**
- Sem mudança de contrato. `ProjectResponseDTO.totalPrice` continua sendo devolvido; a diferença é que passa a estar correto. Nenhum endpoint novo ou removido.

**Testes**
- Novos testes de integração para o total do projeto após cada caminho de mutação e para o limite de consultas na listagem.
