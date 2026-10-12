## Why

O fechamento com os restaurantes é semanal, mas a API só informa o custo por dia da refeição e o total até o término da obra. Além disso, o custo real de alimentação inclui dois itens que o domínio ignora: a taxa de entrega, cobrada só por alguns restaurantes, e um refrigerante que a empresa fornece a cada entrega. O frontend (change `restaurante-custos-periodos`, design D1 e D2) já definiu o contrato novo e depende desta change.

## What Changes

### Taxa de entrega no restaurante (desde o domínio)

- O restaurante ganha `deliveryFee`, opcional, em reais, cobrada por entrega. Quando informada, deve ser maior que zero; senão responde 400 `restaurant.invalid.delivery_fee`. `null` significa restaurante sem taxa.
- `POST/PUT /api/restaurants` aceitam `deliveryFee`. O resumo do restaurante (`RestaurantSummary`, inclusive dentro da refeição e da obra) traz `deliveryFee`.
- A resposta do restaurante (detalhe, listagem paginada e listagem por obra) passa a trazer `meals`: as refeições do restaurante no formato resumido. A listagem busca as refeições de todos os restaurantes da página numa só consulta.

### Refrigerante, entregas e custo por dia detalhado

- A refeição ganha `sodaPrice`, opcional: preço unitário do refrigerante, 1 por entrega. Quando informado, deve ser maior que zero; senão responde 400 `meal.invalid.soda_price`.
- Cada refeição servida conta como uma entrega por dia: `LUNCH` e `DINNER` = 1, `LUNCH_AND_DINNER` = 2 (`deliveriesPerDay`).
- **BREAKING** (valor): `dailyPrice` passa a ser a soma de três parcelas, expostas na resposta: `mealsDailyPrice` (marmitas, o valor atual), `deliveryDailyPrice` (entregas × taxa do restaurante, zero sem taxa) e `sodaDailyPrice` (entregas × `sodaPrice`, zero sem refrigerante). `totalPrice` da refeição e da obra passam a incluir entrega e refrigerante. Sem taxa e sem refrigerante, os valores não mudam.

### Projeção por período de fechamento

- Cada refeição traz `projections` com os períodos `DAY` (1 dia útil), `WEEK` (5), `FORTNIGHT` (10) e `MONTH` (22), nessa ordem. Por período: `workingDays`, `lunchMeals`, `dinnerMeals`, `deliveries`, `sodas` e `price` (`dailyPrice × workingDays`).
- Valores calculados no domínio na leitura; nada é gravado.

### Persistência

- Migração `V5__taxa_entrega_e_refrigerante.sql`: `restaurant.delivery_fee` e `meal.soda_price`, `NUMERIC(19,2)` anuláveis. Dados existentes ficam `null`.

### Não-objetivos

- Registro de fechamentos efetivados (histórico, pago ou a pagar).
- Quantidade de refrigerante diferente de 1 por entrega; taxa de entrega diferente por refeição.
- Calendário real de semanas e meses (a projeção usa dias úteis fixos).

## Capabilities

### New Capabilities

- `restaurant-delivery-fee`: taxa de entrega opcional do restaurante e resposta do restaurante com as refeições que ele serve.
- `meal-cost-breakdown`: entregas por dia, refrigerante por entrega, custo por dia em parcelas (marmitas, entrega, refrigerante) e projeção por período de fechamento.

### Modified Capabilities

(nenhuma; `openspec/specs/` ainda está vazio e `project-meal-plan`, de `refeicoes-por-obra-e-email-opcional`, não foi arquivada. Esta change substitui o requisito "Daily price" daquela capability: `dailyPrice` passa a incluir entrega e refrigerante. O ajuste é registrado no design; ao arquivar, esta change deve ser arquivada depois de `refeicoes-por-obra-e-email-opcional`.)

## Impact

**Código**
- `core/domain`: `Restaurant` (`deliveryFee`), `Meal` (`sodaPrice`, parcelas do custo por dia, projeção), `MealType.deliveriesPerDay()`, novos `BillingPeriod` e `MealProjection`.
- `core/exception`: `InvalidRestaurantException.invalidDeliveryFee`, `InvalidMealException.invalidSodaPrice`.
- `core/repository`: `MealRepository.findByRestaurantIn`.
- `application`: DTOs e mappers de restaurante e refeição (`MealProjectionDTO` novo); `RestaurantUseCase` carrega as refeições.
- `infra`: `RestaurantEntity`, `MealEntity`, mappers de entidade, `MealJpaRepository.findByRestaurantIdIn`, documentação OpenAPI dos controllers de restaurante e refeição.
- `src/main/resources/db/migration/V5__taxa_entrega_e_refrigerante.sql`.

**API**
- Aditiva nos campos (`deliveryFee`, `sodaPrice`, `meals`, parcelas, `projections`).
- **BREAKING** no valor de `dailyPrice`/`totalPrice` quando taxa ou refrigerante são preenchidos.

**Documentação**
- `README.md` (contrato e nota sobre `dailyPrice`) e `app-adm-obras-erd.puml`.

**Integração**
- O frontend (`restaurante-custos-periodos`) publica junto. Os campos novos são anuláveis: reverter o código basta, a `V5` pode ficar.
