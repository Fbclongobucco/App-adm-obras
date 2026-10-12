# Tasks

Pré-requisito: `deploy-viavel` aplicado.

## 1. Caminho de escrita do total do projeto

- [ ] 1.1 Adicionar `recalculateTotal(UUID projectId)` a `ProjectRepository` e implementar no adaptador carregando projeto, refeições e acomodações, deixando o domínio calcular e persistindo o resultado; verificar por teste unitário do adaptador que o total persistido bate com a soma dos componentes
- [ ] 1.2 Criar `ProjectTotalRefresher` em `application/usecase/` dependendo apenas de `ProjectRepository`, e registrar como bean em `UseCaseConfig`; verificar que a aplicação inicia sem dependência ausente
- [ ] 1.3 Injetar o colaborador por construtor em `MealUseCase`, chamando-o após create, update e delete, capturando o `projectId` antes da escrita; verificar por teste que o total do projeto reflete a refeição em cada um dos três casos
- [ ] 1.4 Injetar o colaborador por construtor em `AccommodationUseCase`, com a mesma captura de id prévio à escrita; verificar por teste que o total reflete a acomodação em create, update e delete
- [ ] 1.5 Tratar transferência de refeição e de acomodação entre projetos: capturar os ids antigo e novo antes da escrita e recalcular os dois projetos; verificar por teste que ambos os totais ficam corretos após a transferência
- [ ] 1.6 Garantir que uma mutação rejeitada não deixe o total alterado, por rollback da transação; verificar por teste que uma atualização inválida de refeição mantém refeição e total intactos
- [ ] 1.7 Cobrir o contrato de origem dos totais: total de refeição sempre derivado de preço por quantidade, total de acomodação sempre informado e nunca recalculado, e total negativo de componente rejeitado sem persistir nada

## 2. Leitura em lote de projetos

- [ ] 2.1 Adicionar `findByProjectIdIn` em `MealJpaRepository` e implementar no `ProjectRepositoryAdapter` um helper que coleta os ids da página, executa uma única consulta e agrupa por `(projectId, mealType)` para entregar almoço e janta ao `ProjectEntityMapper.toDomain(entity, lunch, dinner)`, cuja assinatura já aceita esses argumentos
- [ ] 2.2 Aplicar o helper em `findAll(PageRequest)` e em `findAll()` não paginado, removendo as duas consultas por projeto do método `toDomain`; verificar por teste que a listagem continua devolvendo almoço e janta corretos
- [ ] 2.3 Trocar `EAGER` por `LAZY` com `@BatchSize` em `ProjectEntity.employees` e `AccommodationEntity.employees`; verificar por teste de integração que a resposta da listagem é serializada sem `LazyInitializationException`
- [ ] 2.4 Escrever teste que compara o número de consultas ao banco entre uma página pequena e uma página grande, e verificar que os dois valores são iguais, atendendo o requisito de custo constante
- [ ] 2.5 Verificar que a listagem e a leitura de detalhe do mesmo projeto devolvem conteúdo equivalente, e que as validações de paginação (índice negativo e tamanho acima do limite) continuam rejeitando antes de qualquer consulta

## 3. Verificação de conjunto

- [ ] 3.1 Rodar `mvnw test` e confirmar suíte completa verde, incluindo os testes pré-existentes que não foram tocados, com o schema vindo do `V1` de `deploy-viavel`
- [ ] 3.2 Conferir que nenhum endpoint novo foi adicionado e que nenhum contrato de resposta mudou, já que `ProjectResponseDTO.totalPrice` continua sendo devolvido com o mesmo nome e tipo
- [ ] 3.3 Validar o change com `npx @fission-ai/openspec validate endurecer-dominio-e-persistencia --strict` e confirmar que passa
