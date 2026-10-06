## Why

O empilhamento do projeto está sólido — `core/` e `application/` não têm um único import do Spring — mas três costuras estão quebradas:

1. **Totais desatualizados.** `project.total_price` é estado derivado, calculado no domínio a partir das refeições e acomodações, e também gravado como coluna. Só `PUT /api/projects/{id}` o recalcula. `PUT /api/meals/{id}`, `PUT /api/accommodations/{id}` e `DELETE /api/accommodations/{id}` alteram os componentes e deixam o total do projeto parado. A API chega a devolver um total que contradiz as próprias linhas que o compõem. É um bug de integridade visível ao usuário final, não uma questão de estilo.
2. **Sem gestão de schema.** Não existe Flyway, Liquibase nem qualquer DDL versionado. O schema vem 100% de `ddl-auto`. Com `ddl-auto=validate` no perfil `prod`, o primeiro deploy contra um banco vazio falha no boot e não existe caminho suportado para criar as tabelas.
3. **Custo de consulta linear na página.** `ProjectRepositoryAdapter.toDomain` dispara duas queries por projeto para resolver almoço e janta, somadas à coleção `EAGER` de funcionários. `GET /api/projects?size=20` custa ~62 consultas para renderizar 20 linhas.

A estrutura não precisa ser reescrita. Estes são problemas de escrita, de migração e de leitura, todos localizáveis.

## What Changes

### Consistência do total do projeto

- Toda mutação de refeição ou acomodação (create, update, delete, e inclusive mover uma refeição entre projetos) passa a recalcular e persistir `project.total_price` **na mesma transação** da mutação que a invalidou. A coluna passa a ser a fonte da verdade, e o cálculo do domínio é a única origem do valor.
- O caminho de escrita do total passa a existir no adaptador de persistência, orquestrado pela camada de aplicação — preservando `core` e `application` livres de framework.

### Integridade de dados no banco

- Restrição de unicidade em `(project_id, meal_type)` na tabela de refeições, fechando no banco a invariante que hoje só existe em `Project.addMeal`. `findFirstByProjectIdAndMealType` dá lugar a uma busca determinística por unicidade garantida.

### Gestão de schema

- **Flyway** entra como dono do schema. `ddl-auto` é desativado em todos os perfis (`none`), e um conjunto único de migrações passa a criar e versionar o schema.
- Migração baseline `V1` recria o schema completo a partir do que os `@Entity` já descrevem, incluindo índices e a nova restrição de unicidade.
- O H2 de dev/test passa a rodar em `MODE=PostgreSQL`, para que o mesmo conjunto de migrações sirva dev/test e produção.
- O perfil `prod` deixa de confiar em `validate` contra um schema criado fora de banda: a criação passa a ser um passo versionado e explícito.

### Custo de consulta

- A listagem de projetos passa a carregar refeições e funcionários em lote por página, removendo as duas queries por linha. O número de consultas para `GET /api/projects` deixa de crescer com o tamanho da página.
- Coleções `EAGER` de projeto passam a ser `LAZY` com `@BatchSize`, dentro da transação do adaptador.

### Superfície das portas de repositório

- Métodos de consulta sem nenhum call site saem das portas: os 8 localizadores por campo de `Address` (`findByCep`, `findByStreet`, `findByNumber`, `findByNeighborhood`, `findByCity`, `findByState`, `findByCountry`), e os filtros de período e por-entidade de `Project` e `Meal`.
- **`BREAKING`** (interno, sem efeito na API): `ProjectRepository.findByDateBetween` é removido em vez de corrigido. O método batia apenas pela data de **início**, o que não corresponde à intuição de "projetos ativos no período" — sobreposição de intervalos seria a semântica correta. Removendo, o bug desaparece junto com o código morto. Se um filtro por período for necessário no futuro, ele deve nascer já com semântica de sobreposição (`start <= fimDaJanela AND (end IS NULL OR end >= inicioDaJanela)`).

### Hipóteses registradas

- **Total da acomodação continua sendo um valor informado, não derivado.** `Accommodation.totalPrice` é cotado pelo anfitrião e apenas validado como não-negativo; `Meal.totalPrice` é derivado de `price x quantity`. A change **não** unifica as duas regras, porque isso mudaria o contrato da API. Em vez disso, a distinção passa a ser explícita e coberta por teste, para que a soma em `Project.calculateTotalPrice` não dependa de as duas terem a mesma origem.
- **`admin@admim.com` não é typo.** O domínio do usuário não foi consultado e essa string aparece de forma consistente em três arquivos de properties e nos testes. Fica fora do escopo; se for erro de digitação, é uma change separada e trivial.

### Não-objetivos

- **Autenticação.** HTTP Basic sem token, com `csrf.disable()`, é uma escolha defensável para API interna atrás do frontend. A troca por JWT ou sessão no servidor é uma decisão de produto e de superfície de segurança que merece sua própria change.
- **Novos filtros de consulta na API.** Nenhuma query de período, tipo ou custo-center é exposta como endpoint. Hoje só existem `by-project` e `by-cost-center`, e ambos permanecem.
- **Correção do e-mail de bootstrap** `admin@admim.com`.

## Capabilities

### New Capabilities

- `project-cost-totalization`: o total de custo de um projeto reflete, de forma consistente e observável, todas as suas refeições e acomodações em todos os caminhos de mutação, e é persistido como fonte da verdade.
- `meal-assignment-uniqueness`: um projeto tem no máximo uma refeição de cada tipo, e essa unicidade é garantida pelo banco, não apenas pela aplicação.
- `schema-versioning`: o schema do banco é propriedade de migrações versionadas, aplicadas de forma determinística na inicialização, sem depender de `ddl-auto` e com um único conjunto de migrações válido em H2 e PostgreSQL.
- `project-read-performance`: a leitura de projetos, especialmente a listagem paginada, executa um número de consultas que não cresce com o tamanho da página.

### Modified Capabilities

(nenhuma — o projeto ainda não possui specs; todas as capabilities acima são novas)

## Impact

**Dependências**
- `pom.xml`: `+flyway-core` e `+flyway-database-postgresql` (o suporte a PostgreSQL é um módulo separado nas versões atuais do Flyway).

**Configuração**
- `application.properties`, `application-local.properties`, `application-prod.properties`, `application-test.properties`: `spring.jpa.hibernate.ddl-auto=none` em todos; URL do H2 com `;MODE=PostgreSQL`; remoção de `spring.jpa.hibernate.ddl-auto=update` (local) e `=validate` (prod), substituídos pela migração.
- Novo diretório `src/main/resources/db/migration/` com a migração baseline.

**Domínio**
- `core/domain/Project.java`: regra de unicidade de refeição mantida, com a semântica de total documentada.
- `core/domain/Accommodation.java` / `Meal.java`: contrato da origem do total explicitado.

**Portas**
- `core/repository/ProjectRepository.java`: remoção dos métodos sem uso; entrada para recálculo do total.
- `core/repository/AddressRepository.java`, `MealRepository.java`, `EmployeeRepository.java`, `AccommodationRepository.java`, `CostCenterRepository.java`: remoção de localizadores sem call site.

**Infra**
- `infra/adapters/repository/MealRepositoryAdapter.java`, `AccommodationRepositoryAdapter.java`: caminho de escrita do total do projeto; carregamento em lote de refeições.
- `infra/adapters/repository/ProjectRepositoryAdapter.java`: resolução de refeições em lote em vez de duas queries por linha.
- `infra/entities/*Entity.java`: `LAZY` + `@BatchSize` nas coleções; restrição de unicidade em `MealEntity`.
- `infra/adapters/mapper/ProjectEntityMapper.java`: assinatura de `toDomain` passa a receber as refeições já carregadas.

**API**
- Sem mudança de contrato. `ProjectResponseDTO.totalPrice` continua sendo devolvido; o que muda é que ele passa a estar correto. Nenhum endpoint novo ou removido.

**Testes**
- Os testes existentes que fixam o comportamento de `AddressRepository` e dos filtros removidos serão ajustados ou removidos junto com os métodos.
- Novos testes de integração para o total do projeto após cada caminho de mutação, para a restrição de unicidade, para a aplicação de migrações e para o limite de consultas na listagem.