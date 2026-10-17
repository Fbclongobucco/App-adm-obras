## Why

A camada de domínio (`core/domain`) mistura três formas de tipo: classes comuns, `record` e `enum` solto. Os registros de negócio já são classes comuns; os valores de fechamento e auditoria são `record`; `BillingPeriod` é o único enum em arquivo próprio, embora só faça sentido junto de `Meal`. Padronizar o domínio em classes comuns, com cada enum dentro da classe dona, deixa o pacote com uma forma só de leitura e de evolução.

## What Changes

- Os `record` de `core/domain` passam a classes comuns `final`, imutáveis, com a mesma API pública:
  - `AuditStamp`, `ClosingWeek`, `DayClosing`, `MealProjection`, `WeekClosing` e `WeekClosing.Totals`.
  - Mesmo construtor canônico, mesmos acessores no estilo record (`start()`, `status()`, `totals()`...), mesmos métodos de fábrica e de instância.
  - `equals`, `hashCode` e `toString` escritos à mão, com a mesma semântica de igualdade por valor do record.
  - Validação e normalização do construtor compacto de `AuditStamp` mantidas.
- `BillingPeriod` deixa de ser arquivo próprio e passa a `Meal.BillingPeriod`, enum aninhado. Os demais enums já estão aninhados na classe dona e ficam onde estão: `Employee.Role`, `User.Role`, `Meal.MealType`, `MealAdjustment.Type`, `RefreshToken.RevokeReason`, `DayClosing.Status`.
- Simplificação localizada nos tipos convertidos: `WeekClosing` passa a somar os campos dos dias por um único auxiliar, em vez de repetir o mesmo `stream().mapToInt(...).sum()` nos totais da semana e na soma por dia. Sem mudança de regra.
- Ajuste de import nas camadas superiores e nos testes que citam `BillingPeriod` (`MealProjectionDTO`, `MealMapperTest`, `MealTest`).

Nada muda no contrato externo: JSON da API, colunas e valores gravados no banco, `@Enumerated(EnumType.STRING)` das entidades, parâmetros de requisição.

### Premissas registradas

- **Enums continuam enums.** "Embutir nas classes correspondentes" foi lido como aninhar, não como trocar `enum` por classe com constantes. Trocar o tipo exigiria conversores JPA, Jackson e de `@RequestParam`, e quebraria `switch` e `values()` nas camadas superiores.
- **Escopo é `core/domain`.** Os `record` de `core/repository` (`Page`, `PageRequest`, `Sort`, `Sort.Direction`) e `core/security` (`AccessTokenIssuer.IssuedAccessToken`) são contratos de porta, não domínio, e ficam fora.
- **Records continuam em arquivo próprio.** `MealProjection`, `ClosingWeek`, `DayClosing` e `WeekClosing` não são aninhados em outras classes; o pedido de aninhar vale para enums.

### Não-objetivos

- DTOs de `application/dto` (são `record` e continuam).
- Mudança de regra de negócio, de validação ou de mensagem de erro.
- Renomear acessores para o padrão `getX()`.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. Refatoração interna sem mudança de comportamento; o change declara `skip_specs: true`.

## Impact

- **Código de produção alterado:** `core/domain/AuditStamp.java`, `ClosingWeek.java`, `DayClosing.java`, `MealProjection.java`, `WeekClosing.java`, `Meal.java`; `BillingPeriod.java` removido; `application/dto/meal/MealProjectionDTO.java` (import).
- **Testes alterados:** só import de `BillingPeriod` em `MealTest` e `MealMapperTest`. Os demais testes, inclusive os que comparam por `isEqualTo`/`containsExactly` (`WeeklyClosingTest`, `MealTest`, `AuditStampTest`), devem passar sem edição.
- **API, banco, migrações, dependências:** sem impacto.
