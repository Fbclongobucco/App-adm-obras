## Context

Ver `proposal.md` para a motivação. O que restringe as decisões de implementação:

**Restrição estrutural que não pode ser violada.** `core/` e `application/` não contêm nenhum import do Spring — hoje nem javax, jakarta ou org.springframework. Use cases são classes comuns montadas à mão em `infra/config/UseCaseConfig.java`. Toda decisão abaixo respeita essa fronteira: nenhum mecanismo que exija framework entra em `core/` ou `application/`. O preço é que orquestração e transação precisam ser estruturadas, não anotadas.

**A coluna `total_price` já tem hoje exatamente um escritor.** `ProjectEntity.setTotalPrice` só é chamado por `ProjectEntityMapper.apply`, que só é chamado por `ProjectRepositoryAdapter.save/update`. Toda solução precisa criar os escritores que faltam ou deslocar a titularidade do valor.

**`ProjectEntityMapper.toDomain(entity, lunch, dinner)` já recebe as refeições como parâmetro.** A assinatura que eliminate o N+1 já existe; o que falta é preenchê-la em lote em vez de duas queries por linha. Isso reduz o escopo da otimização de leitura.

**A relação entre refeição e projeto é opcional.** `MealEntity.project` aceita `null` (`@JoinColumn(name = "project_id")` sem `nullable = false`) — refeições órfãs são estado legítimo. `Meal.project` no domínio também aceita `null`. Qualquer restrição nova precisa tolerar `project_id` nulo.

**Flyway 12.4.0 é a versão gerenciada** pelo `spring-boot-dependencies:4.1.1`. Verifiquei o conteúdo do jar: o suporte a H2 está dentro do `flyway-core` (`org/flywaydb/core/internal/database/h2/`), enquanto PostgreSQL **não** está e exige o módulo separado `flyway-database-postgresql`. Ambos já constam no repositório local na versão 12.4.0.

**Existe um schema ad-hoc em desenvolvimento.** `compose.yaml` sobe Postgres com volume nomeado `app-adm-obras-pgdata`, e o perfil `prod` usava `ddl-auto` para criá-lo. Esse volume já contém tabelas que nenhuma migração vai criar.

## Goals / Non-Goals

**Goals:**
- Um único caminho estrutural para manter `project.total_price` correto, aplicado a toda mutação de componente, com o mínimo de superfície que possa ser esquecida.
- Número de consultas constante na listagem paginada de projetos.
- Schema reproduzível a partir do repositório, idêntico entre H2 e PostgreSQL, com inicialização determinística.
- Portas de repositório que declarem apenas o que a camada de aplicação usa, sem código morto.

**Non-Goals (limites de design, além do escopo do proposal):**
- Não redesenhar o modelo de agregados. A estrutura atual com o grafo completo em memória é funcional e não é o problema.
- Não introduzir read model, projeção ou CQRS para o cálculo do total. A decisão do usuário foi persistir a coluna.
- Não criar cache de segunda camada nem cache de aplicação.
- Não mexer na autenticação (fora do escopo já declarado).

## Decisions

### D1. O recálculo do total é orquestrado pela camada de aplicação, via colaborador explícito

**Escolha.** Um colaborador da aplicação, `ProjectTotalRefresher`, dependente apenas de `ProjectRepository`, é injetado por construtor em `MealUseCase` e `AccommodationUseCase`. Após cada operação que toca componentes de custo, o caso de uso chama o colaborador. `ProjectRepository` ganha uma operação `recalculateTotal(UUID projectId)`, cuja implementação no adaptador carrega projeto + refeições + acomodações, deixa o domínio calcular e persiste o resultado.

Alternativas descartadas:

- **O adaptador de refeição/acomodação recalcula o total do pai como efeito colateral.** Descartada porque cria um método de porta com efeito fora do contrato declarado, invisível na assinatura — o chamador não consegue ver que `save` também mexe no pai. Pior: um adaptador deixa de ser tradutor e passa a carregar lógica de negócio, o que desfaz a justificativa da porta.
- **Trigger no banco ou coluna gerada.** Descartada por dois motivos. Primeiro, a definição do total ("almoço + janta + acomodações") viraria SQL duplicando regra do domínio, e o domínio é justamente onde ela já está. Segundo, a sintaxe de trigger diverge entre H2 e PostgreSQL, colidindo direto com o requisito de portabilidade do `schema-versioning`.
- **Recalcular na leitura.** Descartada por decisão do usuário: a coluna é a fonte da verdade.

**O ponto fraco desta escolha, e como ele é tratado.** O esquema "o caso de uso precisa lembrar de chamar" pode falhar em silêncio quando alguém escrever um novo caminho de mutação. Mitigações, em ordem de força:

1. A superfície é enumerável e pequena: apenas `MealUseCase` e `AccommodationUseCase` escrevem componentes de custo, e `ProjectUseCase.update` já recalcula por conta própria. Não há um quarto consumidor a proteger.
2. O colaborador entra por construtor. Um caso de uso novo que muta componente de custo não compila sem declará-lo, o que torna a omissão visível em revisão.
3. Testes de integração parametrizados sobre os endpoints de mutação verificam o total após cada operação — a omissão vira teste vermelho, não dado errado silencioso.

**Detalhe que a implementação não pode errar.** Para deletions e transferências, o caso de uso precisa capturar o `projectId` **antes** da escrita, porque depois o filho já não carrega o projeto. Um `delete` que apaga primeiro e depois tenta descobrir o pai vai recalcular o projeto errado ou nenhum. Mesma lógica para transferência: os ids antigo e novo precisam ser capturados antes, e os dois projetos são recalculados.

### D2. Leitura em lote por ids, com a assinatura de mapper já existente

**Escolha.** `ProjectRepositoryAdapter` ganha um helper privado que, dada a lista de entidades de uma página, coleta os ids, executa **uma** consulta `findByProjectIdIn(ids)` em `MealJpaRepository`, agrupa por `(projectId, mealType)` e entrega o almoço e o janta de cada projeto para `ProjectEntityMapper.toDomain(entity, lunch, dinner)`, cuja assinatura já aceita esse terceiro e quarto argumento.

Com isso as duas queries por linha viram uma por página. Vale para `findAll(PageRequest)` e para o `findAll()` não paginado.

**Por que não join fetch.** Trazer as refeições junto no select da página exigiria join com `meal`, e a restriction atual (uma refeição por tipo, e o total depende de `SUM` sobre as linhas) se complicate. Além disso, join-fetching colecionamento com paginação é exatamente o cenário em que o Hibernate cai para paginação em memória — trocar N+1 por memória seria uma troca ruim.

### D3. Coleções de projeto passam a `LAZY` com `@BatchSize`

**Escolha.** `ProjectEntity.employees` e `AccommodationEntity.employees` saem de `EAGER` para `LAZY` com `@BatchSize`. O Hibernate emite uma consulta por página com `IN (...)` em vez de uma por linha.

Contagem esperada para uma página: 1 (count) + 1 (página) + 1 (lote de funcionários) + 1 (lote de refeições) ≈ 4, constante.

**Por que não leave-one-out em D2.** O problema N+1 das refeições é resolvido em D2. O das coleções é `@BatchSize`. Misturar as duas abordagens deixaria duas estratégias de carga no mesmo adaptador.

**Risco assumido.** `LAZY` transforma qualquer acesso a coleção fora da transação em `LazyInitializationException`. Auditado: `toDomain` é chamado só dentro de métodos `@Transactional` do adaptador, e `applyRelations` também. `ProjectEntityMapper.toDomainShallow` não toca coleções. Com `open-in-view=false` já configurado, o domínio devolvido ao caso de uso é um grafo em memória completo e não sofre com isso. Ainda assim, o teste de integração da listagem é o que prova isso, não esta leitura de código.

### D4. `flyway-core` + `flyway-database-postgresql`, H2 em `MODE=PostgreSQL`, um único conjunto de migrações

**Escolha.** Adiciona `flyway-core` e `flyway-database-postgresql` sem versão explícita (herdada do parent, 12.4.0). `spring.jpa.hibernate.ddl-auto=none` em todos os perfis. URL do H2 local e de teste ganha `;MODE=PostgreSQL`. Um diretório `src/main/resources/db/migration/` com `V1__baseline_schema.sql` descrevendo o schema completo.

**Por que `MODE=PostgreSQL` em vez de DDL portátil.** O schema usa `uuid`, `boolean`, `numeric(19,2)` e `timestamp`. Escrever isso em ANSI que funcione nos dois motores dá brittle: `boolean` em H2 nativo é `bit`/`tinyint`, e `uuid` exige o tipo próprio. O modo de compatibilidade alinha a superfície de tipos e mantém **um** conjunto de arquivos, o que é exatamente o ponto de reduzir divergência entre ambientes.

**Por que baseline em vez de gerar de `ddl-auto`.** Exportar o schema que o Hibernate produz e commitar como `V1` garante que a migração nasce da verdade atual, e não de um DDL escrito de memória que pode divergir das entidades. A migração é revisável como código a partir daí.

**O volume de desenvolvimento existente.** O `pgdata` nomeado de quem já rodou com `ddl-auto` contém tabelas que nenhuma migração criará, e o `V1` falhará com "objeto já existe". Duas saídas: `spring.flyway.baseline-on-migrate=true` no perfil de desenvolvimento, ou `docker compose down -v` para recomeçar limpo. **Recomendação:** baseline-on-migrate apenas em dev, nunca em `prod` — em produção, um schema não rastreado é exatamente o estado que o `schema-versioning` existe para impedir. Isso entra como tarefa explícita, com a instrução de recriar o volume.

**Ordem de execução.** Flyway roda antes do `EntityManagerFactory` do Hibernate, então as migrações são aplicadas e depois validadas pelo Hibernate com `ddl-auto=none`. Se uma entidade e a migração divergirem, o erro aparece no boot em vez de em produção.

### D5. Unicidade de refeição no banco, com tradução de exceção

**Escolha.** Restrição `UNIQUE (project_id, meal_type)` em `meal`, declarada na migração `V1` e refletida em `MealEntity` com `@Table(uniqueConstraints = ...)`. `findFirstByProjectIdAndMealType` — o nome `First` documenta que o autor não confiava na unicidade — é substituído por `findByProjectIdAndMealType` retornando `Optional`.

`GlobalExceptionHandler` traduz `DataIntegrityViolationException` para a mesma resposta de erro de validação que a checagem de aplicação produz, para que uma violação que escape da aplicação não vire 500.

**O detalhe de `NULL`.** Refeições órfãs têm `project_id` nulo e são estado legítimo. Em PostgreSQL — e em H2 sob `MODE=PostgreSQL` — valores `NULL` são considerados distintos em restrição de unicidade, então múltiplas refeições órfãs coexistem sem violar. Isso precisa de teste explícito: é o tipo de coisa que muda de comportamento entre motores, e o requisito de portabilidade cobre exatamente isso.

**Invariante de aplicação continua existindo.** `Project.addMeal` continua rejeitando duplicata com mensagem de domínio. A restrição do banco é a segunda linha, não a substituta: a aplicação dá a mensagem útil, o banco garante que ninguém contorne.

### D6. Remoção das portas por evidência de compilador, não por inspeção

**Escolha.** A remoção não é feita por leitura de código, e sim invertendo a verificação: remover os métodos candidatos das portas e deixar o compilador listar quem quebrou. O que não quebrar é código morto confirmado; o que quebrar tem um consumidor legítimo e é preservado.

Isso é mais confiável que `grep` por nome de método, que erra ao confundir a declaração na interface com uma chamada real — a mesma armadilha em que eu caí durante a análise, quando uma contagem inicial sugeriu consumidores onde não havia.

A remoção de `ProjectRepository.findByDateBetween` segue o mesmo caminho: sem call site, sai. `findAll()` não paginado em `ProjectRepository` também não tem consumidor — `ProjectController` só expõe a rota paginada — e segue a mesma regra.

**Limite explícito.** Só sai o que não tem consumidor. `findByProjectId`, `findByCostCenterId`, `findByName`, `findByEmail` e afins continuam: têm call site em caso de uso ou em controller.

## Risks / Trade-offs

**[O total fica desatualizado se um novo caminho de mutação for escrito sem o colaborador]** → Superfície enumerável e pequena; colaborador obrigatório por construtor; teste de integração parametrizado por endpoint de mutação que falha se o total divergir. Risco residual aceito: a proteção é teste + revisão, não o compilador.

**`MODE=PostgreSQL` pode mudar comportamento do H2 em testes que hoje passam com H2 nativo** → Os 261 testes existentes são oDetector. Executar a suíte antes e depois da mudança de URL; qualquer divergência é achado real sobre o modo de compatibilidade, não ruído. Vale checar especificamente tipos de data/hora e ordenação de collation.

**`LAZY` no lugar de `EAGER` pode introduzir `LazyInitializationException` em caminho não coberto por teste** → Todo caminho de leitura de projeto passa a ter teste de integração que serializa a resposta, que é onde a falha apareceria. `open-in-view=false` já está configurado, então não existe Vernizel de segurança que esconda o problema hoje.

**`baseline-on-migrate` mascarando schema divergente em desenvolvimento** → Restrito ao perfil local. Em produção, schema não rastreado deve falhar o boot — é o comportamento pedido pelo `schema-versioning`. Registrado como risco consciente, não como conveniência.

**Restrição de unicidade em `(project_id, meal_type)` colide com dados já existentes** → O `V1` cria o schema do zero, então não há dados prévios no caminho gerenciado. O volume de desenvolvimento legado é tratado separadamente por recriação do volume.

**Perder a query por período de `Project` sem Endpoint que a consuma hoje** → Nenhuma rota depende dela; a API pública não muda. Registrado como risco consciente porque é uma remoção com intenção presumida — se um filtro por período for pedido no futuro, ele nasce com semântica de sobreposição, não com a verificação por data de início que existia aqui.

**Um conjunto de migrações serving dois motores limita o SQL disponível** → É a restrição aceita que comprou a paridade entre ambientes. Se uma necessidade futura exigir funcionalidade só do PostgreSQL, a resposta é `flyway.locations` por perfil naquele ponto, e não voltar atrás agora.

## Migration Plan

Ordem importa porque a migração `V1` precisa refletir o schema final.

1. **Adicionar Flyway e neutralizar o schema automático.** Dependências no `pom.xml`; `ddl-auto=none` nos quatro arquivos de properties; URL do H2 com `MODE=PostgreSQL` em local e test; `baseline-on-migrate` apenas no local. Migrar a suíte existente e rodar: o schema agora vem das migrações, então a primeira execução do `V1` precisa vir antes de qualquer outra coisa.
2. **Escrever `V1__baseline_schema.sql`.** Gerado a partir do schema que o Hibernate produzia, mais a restrição de unicidade de D5 e índices nos pontos de consulta usados (chaves estrangeiras consultadas, `project.os` que já é `unique`, junções de `project_employee` e `accommodation_employee`).
3. **Unicidade de refeição.** Restrição no `V1`, `@Table(uniqueConstraints)` em `MealEntity`, troca de `findFirst...` por `findBy...`, tradução de `DataIntegrityViolationException` em `GlobalExceptionHandler`, mais o teste de `project_id` nulo.
4. **Caminho de escrita do total.** `ProjectRepository.recalculateTotal`, implementação no adaptador, `ProjectTotalRefresher`, injeção em `MealUseCase` e `AccommodationUseCase`, com a captura de ids antes de delete e transferência. Testes de integração por endpoint de mutação.
5. **Leitura em lote.** Helper de carga em lote no adaptador, `LAZY` + `@BatchSize`, e teste que compara o número de consultas entre página pequena e página grande.
6. **Limpeza das portas.** Remoção por evidência de compilador, ajustes nos testes que cobriam os métodos removidos, suíte verde.

**Rollback.** Nenhum passo altera código de aplicação de forma que reverta junto com a migração. Como as migrações passam a ser a fonte do schema, o rollback de schema é `flyway undo` seguido de `repair`, ou restauração do banco. O passo 1 é o ponto de maior risco: remover o `ddl-auto` num banco que já tem schema ad-hoc trava o boot, e é por isso que recriar o volume é tarefa explícita e não opcional.

**Verificação de que a migração é completa.** Comparar, para o mesmo modelo, o schema que `ddl-auto=create-drop` produz e o que o `V1` produz. Divergência entre os dois é o sinal de que o `V1` esqueceu alguma tabela ou coluna.

## Open Questions

Nenhuma que afete specs, abordagem ou quebra de tarefas. Duas uncertainties são deliberadamente adiadas por serem reversíveis sem custo:

- **Tamanho ideal do `@BatchSize`.** O padrão do Hibernate (100) serve; o número real depende de quantos funcionários um projeto tem. Ajustar é mudança de uma constante.
- **Índices além dos de chave estrangeira e do `project.os`.** A lista inicial cobre o que as consultas existentes usam. Se o perfil de consultas mostrar gargalo, o índice seguinte é adicionar um item a uma migração.