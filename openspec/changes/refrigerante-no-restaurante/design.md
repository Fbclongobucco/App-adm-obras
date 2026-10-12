## Context

- Contrato definido pelo frontend em `App-adm-obras-front/openspec/changes/refrigerante-no-restaurante/design.md` (D1, D2 e D3). Esta change o implementa literalmente. Motivação: ver `proposal.md`.
- Hoje (`restaurante-entrega-refrigerante-periodos`): `Restaurant.deliveryFee` no restaurante; `Meal.sodaPrice` na refeição, validado por `InvalidMealException.invalidSodaPrice`; `Meal.getSodaDailyPrice()` e `Meal.projection(...)` leem `sodaPrice`. A `V5` criou `meal.soda_price`.
- Bancos: PostgreSQL em `prod`/compose; H2 2.x com `MODE=PostgreSQL` nos perfis `local` e `test`. A última migração é a `V5`.

## Goals / Non-Goals

**Goals:**
- Uma única fonte do preço do refrigerante: o restaurante, com a regra no domínio. DTO, mapper, entidade e migração acompanham.
- Contrato de leitura da refeição estável: `sodaPrice`, `sodaDailyPrice`, `dailyPrice`, `totalPrice` e `projections` com os mesmos nomes e fórmulas.

**Non-Goals:**
- Exceção por refeição ou quantidade diferente de 1 por entrega.
- Manter compatibilidade de escrita com `sodaPrice` na refeição além de ignorá-lo.

## Decisions

### D1. Domínio
- `Restaurant.sodaPrice` (`BigDecimal`, anulável). `setSodaPrice` aceita `null` e rejeita zero ou negativo com `InvalidRestaurantException.invalidSodaPrice` (`restaurant.invalid.soda_price`), a mesma regra que estava em `Meal`.
- O construtor completo de `Restaurant` ganha `sodaPrice`; as assinaturas anteriores delegam com `null`, para não reescrever chamadas que não usam refrigerante.
- `Meal` remove o atributo, o setter e a sobrecarga do construtor com `sodaPrice`. `getSodaPrice()` passa a devolver `restaurant.getSodaPrice()`. `getSodaDailyPrice()` e `projection(...)` usam `getSodaPrice()`, então leem o restaurante a cada chamada, como a taxa de entrega.
- `InvalidMealException.invalidSodaPrice` e o código `meal.invalid.soda_price` são removidos.
- **Alternativa:** manter `Meal.sodaPrice` como cópia sincronizada. Descartada: duas fontes do mesmo dado.

### D2. API
```
POST/PUT /api/restaurants   + sodaPrice (number | null)
RestaurantSummary           + sodaPrice
Restaurant (resposta)       + sodaPrice
POST/PUT /api/meals         - sodaPrice (ignorado se enviado)
MealSummary.sodaPrice       = restaurant.sodaPrice (somente leitura)
```
- `MealRequestDTO` perde o componente `sodaPrice`. O Jackson do projeto já ignora propriedades desconhecidas (é assim que o front envia campos extras hoje), então um cliente antigo que ainda manda o campo recebe 201/200, sem efeito.
- O README e as descrições OpenAPI registram a mudança.

### D3. Migração `V6__refrigerante_no_restaurante.sql`
```sql
ALTER TABLE restaurant ADD COLUMN soda_price NUMERIC(19,2) NULL;
UPDATE restaurant r SET soda_price = (SELECT MAX(m.soda_price) FROM meal m WHERE m.restaurant_id = r.id);
ALTER TABLE meal DROP COLUMN soda_price;
```
- A subconsulta correlata funciona em H2 (`MODE=PostgreSQL`) e no PostgreSQL. `MAX` ignora `null`: restaurante sem refeição com refrigerante fica `null`.
- `MAX` foi escolhido para não perder preço informado. O efeito colateral (refeições antes sem refrigerante passam a ter) é coberto por teste de migração com dois cenários: restaurante com preços diferentes e restaurante misto.
- **Alternativa:** manter `meal.soda_price` em desuso. Descartada para não deixar coluna morta e enganosa.
- `CostColumnsMigrationTest` (V5) passa a migrar só até a versão 5, porque a coluna que ele verifica deixa de existir na `V6`.

## Risks / Trade-offs

- [Clientes da API ainda enviando `sodaPrice` na refeição] → só existe o front, atualizado junto. O campo é ignorado, não rejeitado.
- [Migração muda o custo de refeições sem refrigerante em restaurantes mistos] → decisão do usuário, registrada na proposta e coberta por teste. Antes do deploy, rodar numa cópia da base e listar os restaurantes afetados: `select restaurant_id from meal group by restaurant_id having count(soda_price) between 1 and count(*) - 1`.
- [`V6` testada só em H2 nos testes automatizados] → SQL restrito a `add column`, `update` com subconsulta correlata e `drop column`, comuns aos dois bancos.

## Migration Plan

1. Rodar a consulta de restaurantes mistos numa cópia da base PostgreSQL e conferir com o usuário.
2. Deploy do backend: Flyway aplica a `V6` no boot.
3. Deploy do front com o campo no restaurante e sem o campo na refeição.
4. Rollback: reverter o código e restaurar a base pelo backup, porque a `V6` remove `meal.soda_price`.
