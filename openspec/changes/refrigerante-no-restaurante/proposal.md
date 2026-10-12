## Why

A change `restaurante-entrega-refrigerante-periodos` colocou o preço do refrigerante na refeição. Na operação, o refrigerante é condição do fornecimento do restaurante, como a taxa de entrega, e o usuário procura o campo no cadastro do restaurante. O frontend (change `refrigerante-no-restaurante`, design D1, D2 e D3) já definiu o contrato novo e depende desta change.

## What Changes

### Refrigerante no restaurante (desde o domínio)

- O restaurante ganha `sodaPrice`, opcional: preço unitário do refrigerante, 1 por entrega, em todas as refeições do restaurante. Quando informado, deve ser maior que zero; senão responde 400 `restaurant.invalid.soda_price`. `null` significa que o restaurante não fornece refrigerante.
- `POST/PUT /api/restaurants` aceitam `sodaPrice`. O resumo do restaurante (`RestaurantSummary`, inclusive dentro da refeição e da obra) e a resposta do restaurante trazem `sodaPrice`.

### Refeição sem preço próprio de refrigerante

- **BREAKING** (escrita): `POST/PUT /api/meals` deixam de ler `sodaPrice`. O campo, se enviado, é ignorado, como qualquer campo desconhecido.
- Na leitura, `MealSummary.sodaPrice` passa a ser o `sodaPrice` do restaurante. `sodaDailyPrice`, `dailyPrice`, `totalPrice` e `projections` mantêm nomes e fórmulas, lendo o preço do restaurante a cada leitura.
- O código `meal.invalid.soda_price` deixa de existir.

### Persistência

- Migração `V6__refrigerante_no_restaurante.sql`: cria `restaurant.soda_price` (`NUMERIC(19,2)`, anulável), copia para cada restaurante o maior `meal.soda_price` das refeições dele e remove `meal.soda_price`.
- Efeito da cópia: num restaurante com refeições com e sem refrigerante, todas passam a ter refrigerante. Decisão do usuário (sem exceção por refeição), coberta por teste de migração.

### Não-objetivos

- Exceção por refeição ("sem refrigerante no jantar").
- Quantidade de refrigerante diferente de 1 por entrega.

## Capabilities

### New Capabilities

(nenhuma)

### Modified Capabilities

`openspec/specs/` ainda está vazio; as duas capabilities abaixo vêm de `restaurante-entrega-refrigerante-periodos`, não arquivada. Esta change deve ser arquivada depois dela.

- `restaurant-delivery-fee`: o restaurante passa a ter também o preço do refrigerante, e o resumo do restaurante o expõe.
- `meal-cost-breakdown`: "Soda per delivery" (preço na refeição) é removido; "Daily price breakdown" passa a ler o refrigerante do restaurante; requisito novo de migração dos preços existentes.

## Impact

**Código**
- `core/domain`: `Restaurant.sodaPrice`; `Meal` perde o atributo e o setter, e `getSodaPrice()` delega ao restaurante.
- `core/exception`: `InvalidRestaurantException.invalidSodaPrice` entra; `InvalidMealException.invalidSodaPrice` sai.
- `application`: `RestaurantRequestDTO`, `RestaurantSummaryDTO`, `RestaurantResponseDTO`, `MealRequestDTO` (sem `sodaPrice`), `RestaurantMapper`, `MealMapper`.
- `infra`: `RestaurantEntity`, `MealEntity`, mappers de entidade, documentação OpenAPI dos controllers de restaurante e refeição.
- `src/main/resources/db/migration/V6__refrigerante_no_restaurante.sql`.

**API**
- Aditiva em restaurante (`sodaPrice`). **BREAKING** só na escrita de refeição; a leitura mantém os nomes.

**Documentação**
- `README.md` (contrato e nota da mudança) e `app-adm-obras-erd.puml` (`soda_price` sai de `meal` e entra em `restaurant`).

**Integração**
- O frontend publica junto. Rollback exige restaurar a base pelo backup, porque a `V6` remove `meal.soda_price`.
