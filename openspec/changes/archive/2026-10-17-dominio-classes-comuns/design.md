## Context

Ver proposal.md (Why). Estado atual levantado no código:

- `core/domain` tem 5 `record` de topo (`AuditStamp`, `ClosingWeek`, `DayClosing`, `MealProjection`, `WeekClosing`) e 1 aninhado (`WeekClosing.Totals`).
- Único enum de topo: `BillingPeriod`, usado por `Meal`, `MealProjection`, `application/dto/meal/MealProjectionDTO` e pelos testes `MealTest` e `MealMapperTest`.
- As camadas superiores consomem os records só por acessores (`week.start()`, `day.status()`, `closing.totals()`, `projection.period()`...) e construtores (`new AuditStamp(...)` em `infra/adapters/mapper/AuditMapping`).
- Testes dependem da igualdade por valor: `WeeklyClosingTest` (`isEqualTo(new ClosingWeek(...))`, `containsExactly` de semanas), `MealTest` (`isEqualTo(new MealProjection(...))`), `AuditStampTest`.
- Não há pattern matching de record (`instanceof X(var ...)`), `usingRecursiveComparison` nem serialização Jackson direta desses tipos: todos passam por DTO.
- Entidades JPA guardam os enums aninhados com `@Enumerated(EnumType.STRING)`; os DTOs expõem os enums no JSON pelo `name()`.
- Java 25.

## Goals / Non-Goals

**Goals:**
- Compatibilidade de código-fonte para tudo fora de `core/domain`, exceto o import de `BillingPeriod`.
- Mesma semântica de valor: igualdade, hash e imutabilidade.

**Non-Goals:**
- Converter enums em classes.
- Lombok ou outra dependência para gerar `equals`/`hashCode`.

## Decisions

### 1. Manter os acessores no estilo record

Os acessores continuam `start()`, `status()`, `createdAt()` etc., sem `getX()`.

- **Por quê:** é o que preserva as camadas superiores e os testes sem edição. `Audited` já chama `audit.createdAt()`; mappers e testes usam referências de método como `DayClosing::status` e `ClosingWeek::start`.
- **Alternativa descartada:** `getX()` como nas entidades de domínio. Exigiria mexer em mappers, `Audited` e testes, sem ganho funcional.

### 2. Classe `final`, campos `private final`, `equals`/`hashCode`/`toString` à mão

- `equals` compara todos os componentes, como o record: `Objects.equals` para referências e `==` para primitivos. `BigDecimal` segue com `equals` (escala importa), igual ao comportamento atual.
- `hashCode` via `Objects.hash(...)`.
- `toString` no formato `Nome[campo=valor, ...]`, igual ao do record, para manter mensagens de falha de teste legíveis.
- **Alternativa descartada:** omitir `equals`/`hashCode`. Quebraria `WeeklyClosingTest` e `MealTest`, que comparam por valor.

### 3. Construtor canônico público, com a validação do construtor compacto

`AuditStamp` mantém a ordem atual: valida nulos, trunca em microssegundos e então valida `updatedAt >= createdAt`. As exceções lançadas são as mesmas (`InvalidAuditStampException`).

### 4. Coleções imutáveis na construção

`DayClosing.adjustmentIds` e `WeekClosing.days` passam por `List.copyOf` no construtor. Hoje todos os chamadores já passam listas imutáveis (`List.of()`, `toList()`, `List.copyOf`), então a cópia não muda resultado; só fixa a imutabilidade na classe, que o record não garantia. Com isso, `WeekClosing.of` deixa de precisar do próprio `List.copyOf`.

### 5. `BillingPeriod` aninhado em `Meal`

`Meal.BillingPeriod`, enum `public static` (implícito), com o mesmo nome, constantes, ordem e `getWorkingDays()`.

- **Por quê:** só `Meal` usa o período; a ordem das constantes define a ordem de `Meal.getProjections()` e do JSON, por isso fica intacta.
- O nome serializado continua `DAY`, `WEEK`, `FORTNIGHT`, `MONTH`; o OpenAPI gerado mantém o mesmo enum.
- **Alternativa descartada:** aninhar em `MealProjection`. `Meal.projection(...)` recebe o período como parâmetro de entrada; a classe dona natural é `Meal`.

### 6. Simplificação em `WeekClosing`

Os totais da semana e a soma por dia repetem cinco agregações sobre `List<DayClosing>`. Um auxiliar privado único, por exemplo `sumInt(List<DayClosing>, ToIntFunction<DayClosing>)` e `sumPrice(List<DayClosing>)`, substitui as repetições. A regra de situação somada (`sum`) não muda.

## Risks / Trade-offs

- [`equals`/`hashCode` escritos à mão podem esquecer um campo] → Revisar campo a campo contra a assinatura do record atual; os testes de igualdade existentes cobrem `ClosingWeek` e `MealProjection`.
- [Mais código boilerplate que o record] → Aceito pelo pedido de padronização; restrito a 6 tipos pequenos.
- [Algum consumidor fora do código Java depender de `Class.isRecord()` ou reflexão de componentes] → Não encontrado no repositório; Jackson não serializa esses tipos diretamente.

## Migration Plan

Refatoração em um commit. Sem migração de banco nem de API. Rollback: reverter o commit.
