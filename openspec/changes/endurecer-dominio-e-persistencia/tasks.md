## 1. Flyway e neutralização do schema automático

- [ ] 1.1 Adicionar `flyway-core` e `flyway-database-postgresql` ao `pom.xml` sem versão explícita (herdada do parent, 12.4.0) e verificar com `mvnw dependency:tree` que ambas resolvem para 12.4.0
- [ ] 1.2 Definir `spring.jpa.hibernate.ddl-auto=none` em `application.properties`, `application-local.properties`, `application-prod.properties` e `application-test.properties`, removendo os valores `update` e `validate` existentes, e verificar por grep que nenhum perfil mantém um valor de `ddl-auto` diferente de `none`
- [ ] 1.3 Adicionar `;MODE=PostgreSQL` à URL do H2 em `application-local.properties` e `application-test.properties`, e verificar que a aplicação sobe e responde a `/api/auth/me` sem erro de dialecto ou tipo
- [ ] 1.4 Executar a suíte existente completa (`mvnw test`) e verificar que os 261 testes continuam verdes sob o novo modo de compatibilidade; qualquer falha é achado real sobre `MODE=PostgreSQL`, e não deve ser suprimido
- [ ] 1.5 Definir `spring.flyway.baseline-on-migrate=true` apenas em `application-local.properties`, e verificar por grep que `prod` e `test` não a recebem
- [ ] 1.6 Documentar no README da change `infra-docker-compose-e-der`, ou registrar explicitamente na documentação operacional, que qualquer ambiente de desenvolvimento que possuía o volume `app-adm-obras-pgdata` deve recriá-lo (`docker compose down -v`) antes de subir com o novo Compose, pois esse volume contém schema ad-hoc anterior e não será mais gerenciado pelo próprio Compose; verificar que a instrução está presente e correta, dado que o `compose.yaml` deixa de definir o serviço Postgres.

## 2. Migração baseline do schema

- [ ] 2.1 Gerar o schema de referência subindo a aplicação com `ddl-auto=create-drop` num banco limpo e exportar o DDL resultante, para ter a verdade atual das entidades como ponto de partida
- [ ] 2.2 Criar `src/main/resources/db/migration/V1__baseline_schema.sql` com todas as tabelas, colunas, tipos, chaves estrangeiras e índices refletindo as entidades atuais, usando tipos portáteis entre H2 e PostgreSQL
- [ ] 2.3 Adicionar à migração índices para os pontos de consulta já em uso: chaves estrangeiras consultadas por filtro, `project.os` (já `unique`) e as tabelas de junção `project_employee` e `accommodation_employee`
- [ ] 2.4 Verificar que a aplicação sobe contra um banco vazio e que o `V1` é aplicado; comparar tabela a tabela, coluna a coluna, o schema gerado por `ddl-auto=create-drop` contra o produzido pelo `V1`, e resolver toda divergência encontrada
- [ ] 2.5 Verificar idempotência: reiniciar a aplicação contra o banco já migrado e confirmar que nenhuma migração é reaplicada, que nenhum dado é perdido e que a suíte completa continua verde

## 3. Unicidade de refeição no banco

- [ ] 3.1 Declarar `UNIQUE (project_id, meal_type)` na tabela `meal` dentro do `V1` e espelhar em `MealEntity` com `@Table(uniqueConstraints = ...)`, e verificar que o Hibernate com `ddl-auto=none` não acusa divergência de schema no boot
- [ ] 3.2 Substituir `findFirstByProjectIdAndMealType` por `findByProjectIdAndMealType` retornando `Optional` em `MealJpaRepository` e no adaptador, e verificar que os testes de leitura de projeto continuam verdes
- [ ] 3.3 Traduzir `DataIntegrityViolationException` para a mesma resposta de erro de validação em `GlobalExceptionHandler`, e verificar por teste de integração que uma violação de unicidade persistente responde 400 e não 500
- [ ] 3.4 Cobrir em teste que múltiplas refeições órfãs com `project_id` nulo coexistem sem violar a restrição, já que `NULL` é distinto em unicidade nos dois motores e a regra depende disso
- [ ] 3.5 Cobrir em teste que uma alteração que troque almoço e janta na mesma requisição é aceita, e que um conjunto final com dois almoços é rejeitado mantendo o conjunto anterior

## 4. Caminho de escrita do total do projeto

- [ ] 4.1 Adicionar `recalculateTotal(UUID projectId)` a `ProjectRepository` e implementar no adaptador carregando projeto, refeições e acomodações, deixando o domínio calcular e persistindo o resultado; verificar por teste unitário do adaptador que o total persistido bate com a soma dos componentes
- [ ] 4.2 Criar `ProjectTotalRefresher` em `application/usecase/` dependendo apenas de `ProjectRepository`, e registrar como bean em `UseCaseConfig`; verificar que a aplicação inicia sem dependência ausente
- [ ] 4.3 Injetar o colaborador por construtor em `MealUseCase`, chamando-o após create, update e delete, capturando o `projectId` antes da escrita; verificar por teste que o total do projeto reflete a refeição em cada um dos três casos
- [ ] 4.4 Injetar o colaborador por construtor em `AccommodationUseCase`, com a mesma captura de id prévio à escrita; verificar por teste que o total reflete a acomodação em create, update e delete
- [ ] 4.5 Tratar transferência de refeição e de acomodação entre projetos: capturar os ids antigo e novo antes da escrita e recalcular os dois projetos; verificar por teste que ambos os totais ficam corretos após a transferência
- [ ] 4.6 Garantir que uma mutação rejeitada não deixe o total alterado, por rollback da transação; verificar por teste que uma atualização inválida de refeição mantém refeição e total intactos
- [ ] 4.7 Cobrir o contrato de origem dos totais: total de refeição sempre derivado de preço por quantidade, total de acomodação sempre informado e nunca recalculado, e total negativo de componente rejeitado sem persistir nada

## 5. Leitura em lote de projetos

- [ ] 5.1 Adicionar `findByProjectIdIn` em `MealJpaRepository` e implementar no `ProjectRepositoryAdapter` um helper que coleta os ids da página, executa uma única consulta e agrupa por `(projectId, mealType)` para entregar almoço e janta ao `ProjectEntityMapper.toDomain(entity, lunch, dinner)`, cuja assinatura já aceita esses argumentos
- [ ] 5.2 Aplicar o helper em `findAll(PageRequest)` e em `findAll()` não paginado, removendo as duas consultas por projeto do método `toDomain`; verificar por teste que a listagem continua devolvendo almoço e janta corretos
- [ ] 5.3 Trocar `EAGER` por `LAZY` com `@BatchSize` em `ProjectEntity.employees` e `AccommodationEntity.employees`; verificar por teste de integração que a resposta da listagem é serializada sem `LazyInitializationException`
- [ ] 5.4 Escrever teste que compara o número de consultas ao banco entre uma página pequena e uma página grande, e verificar que os dois valores são iguais, atendendo o requisito de custo constante
- [ ] 5.5 Verificar que a listagem e a leitura de detalhe do mesmo projeto devolvem conteúdo equivalente, e que as validações de paginação (índice negativo e tamanho acima do limite) continuam rejeitando antes de qualquer consulta

## 6. Limpeza das portas de repositório

- [ ] 6.1 Remover em bloco os métodos candidatos das portas (`AddressRepository` com os 8 localizadores por campo, filtros de período e por entidade em `ProjectRepository` e `MealRepository`, `findAll()` não paginado em `ProjectRepository`) e usar os erros de compilação como inventário de quem realmente consome cada um
- [ ] 6.2 Restaurar todo método que o compilador indicar ter consumidor legítimo, e remover de porta e adaptador apenas o que o compilador confirmar sem uso, incluindo `ProjectRepository.findByDateBetween` e a implementação correspondente em `ProjectJpaRepository`
- [ ] 6.3 Remover das interfaces `*JpaRepository` as queries de filtro que ficaram sem porta correspondente, e verificar que nenhuma classe de produção referencia método removido
- [ ] 6.4 Ajustar ou remover os testes que cobriam os métodos apagados, e verificar que a suíte completa fica verde
- [ ] 6.5 Verificar por inspeção final que nenhuma porta declara método sem consumidor e que `core/` e `application/` continuam sem import de Spring, jakarta ou javax

## 7. Verificação de conjunto

- [ ] 7.1 Rodar `mvnw test` e confirmar suíte completa verde, incluindo os testes pré-existentes que não foram tocados
- [ ] 7.2 Subir a aplicação contra um banco vazio no perfil `prod` e verificar que o boot aplica o `V1` e conclui sem erro, cobrindo o cenário de primeiro deploy que era bloqueante antes desta change
- [ ] 7.3 Conferir que nenhum endpoint novo foi adicionado e que nenhum contrato de resposta mudou, já que `ProjectResponseDTO.totalPrice` continua sendo devolvido com o mesmo nome e tipo