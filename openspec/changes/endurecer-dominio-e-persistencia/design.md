## Context

Ver `proposal.md` para a motivação. O que restringe as decisões de implementação:

**Restrição estrutural que não pode ser violada.** `core/` e `application/` não contêm nenhum import do Spring — hoje nem javax, jakarta ou org.springframework. Use cases são classes comuns montadas à mão em `infra/config/UseCaseConfig.java`. Toda decisão abaixo respeita essa fronteira: nenhum mecanismo que exija framework entra em `core/` ou `application/`. O preço é que orquestração e transação precisam ser estruturadas, não anotadas.

**A coluna `total_price` já tem hoje exatamente um escritor.** `ProjectEntity.setTotalPrice` só é chamado por `ProjectEntityMapper.apply`, que só é chamado por `ProjectRepositoryAdapter.save/update`. Toda solução precisa criar os escritores que faltam ou deslocar a titularidade do valor.

**`ProjectEntityMapper.toDomain(entity, lunch, dinner)` já recebe as refeições como parâmetro.** A assinatura que eliminate o N+1 já existe; o que falta é preenchê-la em lote em vez de duas queries por linha. Isso reduz o escopo da otimização de leitura.

**A relação entre refeição e projeto é opcional.** `MealEntity.project` aceita `null` (`@JoinColumn(name = "project_id")` sem `nullable = false`) — refeições órfãs são estado legítimo. `Meal.project` no domínio também aceita `null`. Qualquer restrição nova precisa tolerar `project_id` nulo.

## Goals / Non-Goals

**Goals:**
- Um único caminho estrutural para manter `project.total_price` correto, aplicado a toda mutação de componente, com o mínimo de superfície que possa ser esquecida.
- Número de consultas constante na listagem paginada de projetos.

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

## Risks / Trade-offs

**[O total fica desatualizado se um novo caminho de mutação for escrito sem o colaborador]** → Superfície enumerável e pequena; colaborador obrigatório por construtor; teste de integração parametrizado por endpoint de mutação que falha se o total divergir. Risco residual aceito: a proteção é teste + revisão, não o compilador.

**`LAZY` no lugar de `EAGER` pode introduzir `LazyInitializationException` em caminho não coberto por teste** → Todo caminho de leitura de projeto passa a ter teste de integração que serializa a resposta, que é onde a falha apareceria. `open-in-view=false` já está configurado, então não existe Vernizel de segurança que esconda o problema hoje.

## Migration Plan

Pré-requisito: `deploy-viavel` aplicado (schema versionado por Flyway, `ddl-auto=none`, H2 em `MODE=PostgreSQL`).

1. **Caminho de escrita do total.** `ProjectRepository.recalculateTotal`, implementação no adaptador, `ProjectTotalRefresher`, injeção em `MealUseCase` e `AccommodationUseCase`, com a captura de ids antes de delete e transferência. Testes de integração por endpoint de mutação.
2. **Leitura em lote.** Helper de carga em lote no adaptador, `LAZY` + `@BatchSize`, e teste que compara o número de consultas entre página pequena e página grande.

**Rollback.** Os dois passos são só de código, sem migração de schema; reverter o commit basta. Totais gravados corretamente pelo passo 1 continuam válidos após um rollback.

## Open Questions

Nenhuma que afete specs, abordagem ou quebra de tarefas. Uma incerteza é deliberadamente adiada por ser reversível sem custo:

- **Tamanho ideal do `@BatchSize`.** O padrão do Hibernate (100) serve; o número real depende de quantos funcionários um projeto tem. Ajustar é mudança de uma constante.
