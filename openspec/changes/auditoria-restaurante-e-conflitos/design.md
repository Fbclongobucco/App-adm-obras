## Context

Ver `proposal.md` para a motivação e `specs/` para os requisitos. O que restringe as decisões:

**Fronteira de framework.** `core/` e `application/` não importam Spring nem Jakarta. Casos de uso são classes comuns montadas em `infra/config/UseCaseConfig.java`. `Clock` já é injetado assim em `AuthUseCase`.

**Transação no adaptador.** O `@Transactional` está nos métodos dos adaptadores (`infra/adapters/repository/*`), não nos casos de uso. Uma verificação feita no caso de uso seguida de gravação no adaptador não é atômica.

**Restrições nomeadas.** O `V1` nomeia todas as restrições (`uk_project_os`, `uk_client_email`, `uk_employee_email`, `uk_employee_cpf`, `uk_app_user_email`, `uk_meal_project_type`, `fk_*`). O nome chega na causa da `DataIntegrityViolationException`.

**Refeição e obra.** O vínculo vive em `meal.project_id` + `meal_type`; `project` não tem coluna de almoço nem de janta. `ProjectRepositoryAdapter.assignMeals` reescreve `meal.project_id` a cada gravação de obra a partir de `lunchId`/`dinnerId`.

**Mapper de colaborador.** `EmployeeEntityMapper.apply` chama `CostCenterEntityMapper.apply` sobre a entidade de centro de custo já ligada ao colaborador, sobrescrevendo nome e CNPJ do centro antigo.

## Goals / Non-Goals

**Goals:**
- Datas de auditoria como dado do domínio, testáveis sem banco e sem relógio real.
- Ordenação expressa no `core`, traduzida para o Spring só no adaptador.
- Código de erro específico para duplicidade mesmo sob concorrência.

**Non-Goals:**
- Auditoria por JPA (`@CreationTimestamp`, `@EntityListeners`, Spring Data Auditing).
- Ordenação genérica para todos os repositórios nesta etapa.
- Bloqueio pessimista ou otimista para evitar corridas de duplicidade.

## Decisions

### D1. Datas de auditoria vivem no domínio e são definidas pelo caso de uso

**Escolha.** As datas formam o valor `AuditStamp(createdAt, updatedAt)` do `core`, que rejeita nulos e `updatedAt` anterior a `createdAt` e trunca para microssegundos (precisão de `timestamp(6)`, para a resposta da gravação bater com a leitura). Os domínios auditados estendem a base `Audited`, que guarda o `AuditStamp` e expõe `markCreated(now)`, `touch(now)`, `touchFrom(anterior, now)` e `restoreAudit(stamp)`. Os casos de uso recebem `Clock` por construtor (via `UseCaseConfig`): na criação chamam `markCreated`; na alteração, `touchFrom(existente, now)`, que preserva a criação lida mesmo quando o mapper monta um objeto novo.

*Ajuste na implementação:* o plano era receber as datas no construtor de cada domínio. Isso mudaria a assinatura usada em centenas de pontos de teste sem ganho de regra; a base `Audited` mantém a validação no domínio e o banco garante `not null`.

**Alternativas descartadas:**
- `@CreationTimestamp`/`@UpdateTimestamp` ou Spring Data Auditing: a regra ficaria só na `infra`, invisível e não testável no domínio. Também mudaria `updatedAt` em qualquer flush, inclusive no recálculo do total da obra (`endurecer-dominio-e-persistencia`), o que viola o requisito de a data da obra refletir só alterações da própria obra.
- Trigger no banco: diverge entre H2 e PostgreSQL e duplica regra fora do domínio.

**Consequência.** Escritas indiretas (recálculo de total, `assignMeals`, vínculo de hospedagem) não passam pelo `touch` e não alteram `updatedAt`. Os mappers de entidade copiam as datas sem gerá-las.

### D2. Ordenação como valor do `core`

**Escolha.** `PageRequest` ganha um `Sort` opcional (`record Sort(String field, Direction direction)`), criado a partir do texto da requisição e validado contra uma lista de campos permitidos por recurso. Campo ou direção inválidos lançam `InvalidPageException.invalidSort` (`page.invalid.sort`, 400). `SpringPageSupport.toPageable` traduz o campo do domínio para a propriedade da entidade e acrescenta `id` como desempate fixo.

**Alternativa descartada:** aceitar `org.springframework.data.domain.Sort` no controller e repassá-lo. Exporia o Spring à porta do repositório, quebrando a fronteira, e permitiria ordenar por qualquer propriedade da entidade.

**Lista permitida para obra:** `updatedAt`, `createdAt`, `os`, `startDate`. A tradução campo → propriedade fica no adaptador de obra.

### D3. Restaurante como entidade; refeição mantém `project_id` desnormalizado

**Escolha.** Tabela `restaurant` (`id`, `name`, `phone`, `address_id`, `project_id NOT NULL`, datas). `meal` ganha `restaurant_id NOT NULL` e perde `restaurant_name`. **`meal.project_id` permanece**, preenchido pelo adaptador a partir do restaurante, para que `uk_meal_project_type` continue garantindo uma refeição de cada tipo por obra no banco.

A consistência entre `meal.project_id` e `restaurant.project_id` é garantida por três pontos:
- o adaptador de refeição grava `project_id` sempre a partir do restaurante;
- o restaurante não muda de obra enquanto tiver refeições (`restaurant.in_use`);
- `ProjectRepositoryAdapter.assignMeals` deixa de reescrever `meal.project_id`.

**Alternativa descartada:** remover `meal.project_id` e verificar a unicidade por junção. A regra sairia do banco e perderia a garantia sob concorrência exigida por `meal-assignment-uniqueness`.

**Criação de obra com `lunchId`/`dinnerId`.** Uma obra nova ainda não tem restaurantes, logo nenhuma refeição pode pertencer a ela: qualquer `lunchId`/`dinnerId` na criação responde 400 `project.invalid.meal`.

**`lunchId`/`dinnerId` no PUT de obra.** Continuam aceitos para compatibilidade e só são validados: a refeição precisa existir, ter o tipo do campo e já pertencer à obra (por restaurante). Não movem mais refeições entre obras. Uma refeição de outra obra responde 400 `project.invalid.meal`. Omitir os campos não desvincula nada, porque o vínculo agora vem do restaurante.

> Isso muda um comportamento descrito na proposta como "só documentar". A mudança é consequência direta do restaurante: a obra da refeição deixa de ser definida pelo PUT da obra. `employeeIds` e `accommodationIds` mantêm a semântica de substituição completa, que passa a ser documentada.

### D4. Endereço do restaurante segue o padrão de cliente

**Escolha.** Endereço embutido na requisição, gravado em `address` com cascade a partir de `RestaurantEntity`, como em `ClientEntity`. Isso evita repetir o problema do `MealEntity`, em que o endereço fica órfão na exclusão por não ter cascade.

### D5. Conflitos: verificação no caso de uso e tradução pelo nome da restrição

**Escolha.** Duas camadas:
1. **Caso de uso.** Antes de gravar, consulta a porta (`existsByOsAndIdNot`, `existsByCpfAndIdNot` etc.; dependentes via `countBy…`). Se houver conflito, lança `ConflictException` do `core`, que o `GlobalExceptionHandler` mapeia para 409 com o `errorCode` da exceção.
2. **Handler.** Captura `DataIntegrityViolationException`, procura o nome da restrição na causa e consulta uma tabela fixa: `uk_project_os` → `project.duplicate.os`, `uk_employee_cpf` → `employee.duplicate.cpf`, `fk_employee_cost_center` → `cost_center.in_use` etc. Sem correspondência, usa `data.integrity_violation`. A mensagem nunca inclui o texto do banco.

**Por quê.** A camada 1 dá mensagem precisa e evita gravação inútil no caso comum. A camada 2 cobre a corrida entre a verificação e a gravação (D, Context: transação no adaptador) sem exigir bloqueio, e mantém o código específico.

**Substituição.** A tarefa 3.3 de `deploy-viavel` (violação → 400) passa a ser atendida por este handler com 409. Os cenários de `meal-assignment-uniqueness` que dizem "erro de validação" passam a ser verificados com 409 `meal.duplicate.type`.

### D6. Correção da troca de centro de custo

**Escolha.** `EmployeeEntityMapper.apply` passa a receber a `CostCenterEntity` de destino, carregada por id no adaptador (`getReferenceById`), e apenas troca a referência. `CostCenterEntityMapper.apply` deixa de ser chamado a partir do colaborador. O teste de regressão compara nome e CNPJ dos dois centros antes e depois.

## Risks / Trade-offs

- [Conflito de merge com `deploy-viavel` e `endurecer-dominio-e-persistencia`, que tocam `MealEntity`, `MealUseCase`, `AccommodationUseCase` e `ProjectRepositoryAdapter`] → Aplicar este change depois dos dois. Se a ordem não for possível, aplicar antes e ajustar as tarefas abertas daqueles changes, registrando o ajuste neles.
- [**BREAKING** em `MealRequestDTO`] → Não há consumidor além do frontend novo, que ainda não usa refeições. Registrar no README.
- [Refeição existente com `project_id` nulo não tem obra para criar restaurante] → O `V3` falha nesse caso (ver Migration Plan). `local`/`test` partem de banco vazio; `prod` ainda não foi implantado (`deploy-viavel` 6.1 aberto).
- [Duplicidade verificada em duas camadas pode divergir] → A tabela de restrições do handler é coberta por teste que força a violação no banco, contornando o caso de uso, para cada código.

## Migration Plan

`V3__auditoria_e_restaurante.sql`, em uma migração:

1. Adiciona `created_at`/`updated_at` (`timestamp(6) with time zone`) às tabelas auditadas, preenche com `current_timestamp` e então torna as colunas `not null`.
2. Cria `restaurant` com índice em `project_id`.
3. Insere um restaurante para cada par distinto (`project_id`, `restaurant_name`) das refeições com `project_id` não nulo, reaproveitando o id e o endereço da refeição de menor id do par. *Ajuste na implementação:* gerar UUID em SQL não é portável entre H2 e PostgreSQL; reaproveitar o id é seguro porque ids não colidem entre tabelas, e o endereço deixa de ser compartilhado porque `meal.address_id` é removido no passo 6.
4. Preenche `meal.restaurant_id` pela junção (`project_id`, `restaurant_name`).
5. Torna `meal.restaurant_id` e `meal.project_id` `not null` e adiciona `fk_meal_restaurant`. Se houver refeição sem obra, o passo falha e a migração inteira é revertida. Antes de rodar em banco com dados, executar `select count(*) from meal where project_id is null` e vincular ou remover essas refeições.
6. Remove `meal.restaurant_name` e `meal.address_id` (com `fk_meal_address` e `idx_meal_address`, nessa ordem: no H2 o índice pertence à restrição). Endereços das demais refeições de cada par ficam órfãos, como já ficam hoje, e podem ser limpos depois.

**Rollback:** Flyway não reverte. Em `prod`, restaurar o backup anterior. Em `local`/`test`, o H2 é recriado no reinício.

## Open Questions

- Remover os endereços órfãos de refeição numa migração futura ou deixá-los: não afeta comportamento.
