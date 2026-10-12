## Why

A refeição hoje é um lançamento com data, quantidade e preço únicos (`Meal.date`, `quantity`, `price`), um registro de almoço e outro de jantar por obra. Na operação, a refeição é a alimentação contratada para a obra inteira: restaurante, refeições servidas e quantidade e preço por dia. O custo que interessa é o custo por dia e o total estimado até o término da obra, e isso não existe na API. O frontend (change `melhorias-design-refeicoes-cadastros`, design D1, D2 e D7) já definiu o contrato novo e depende desta change.

O e-mail de cliente e de colaborador é obrigatório no domínio, mas nem todo cliente ou colaborador tem e-mail. O cadastro fica bloqueado ou recebe e-mail fictício.

## What Changes

### Refeição como configuração da obra (desde o domínio)

- **BREAKING** (API): `MealType` passa a ser `LUNCH | DINNER | LUNCH_AND_DINNER`.
- **BREAKING** (API): a refeição perde `date`, `quantity` e `price`. Passa a ter `lunchQuantity`/`lunchPrice` e `dinnerQuantity`/`dinnerPrice`, obrigatórios conforme o tipo (quantidade inteira de 1 a 10.000, preço maior que zero). Campos de refeição não servida são ignorados e devolvidos `null`.
- A resposta da refeição traz `dailyPrice` (soma de quantidade × preço das refeições servidas), `workingDays` (segunda a sexta entre início e término da obra, inclusive, sem feriados) e `totalPrice` (`dailyPrice × workingDays`). Sem término da obra, `workingDays` e `totalPrice` vêm `null`. Os valores são calculados no domínio a partir da obra atual, não gravados.
- Unicidade: cada obra tem no máximo uma refeição que inclua almoço e no máximo uma que inclua jantar. `LUNCH_AND_DINNER` ocupa as duas. Violação responde 409 `meal.duplicate.type`, na aplicação e no banco.
- Novos `errorCode`: `meal.invalid.lunch_quantity`, `meal.invalid.lunch_price`, `meal.invalid.dinner_quantity`, `meal.invalid.dinner_price`. Deixam de existir `meal.invalid.price`, `meal.invalid.quantity`, `meal.invalid.date` e `meal.invalid.date.future`.
- **BREAKING** (API): a obra perde `lunch` e `dinner` na resposta e ganha `meals` (lista resumida). `ProjectRequestDTO` perde `lunchId` e `dinnerId`: a refeição se vincula à obra pelo restaurante.
- O `totalPrice` da obra passa a somar o `totalPrice` de cada refeição (nulo conta como zero) e o das hospedagens.
- Migração `V4` converte as refeições existentes (`LUNCH` para os campos de almoço, `DINNER` para os de jantar), remove `date`, `quantity`, `price` e `total_price` de `meal` e troca a unicidade `(project_id, meal_type)` pela de cobertura.

### E-mail opcional em cliente e colaborador

- E-mail `null` ou em branco é gravado como `null`. Quando informado, segue validado (`client.invalid.email`, `employee.invalid.email`) e único (409 `client.duplicate.email`, `employee.duplicate.email`). Vários registros sem e-mail não conflitam.
- `V4` torna `client.email` e `employee.email` anuláveis. As restrições únicas continuam valendo para valores não nulos.
- Usuário (`User`) não muda: o e-mail é o login.

### Não-objetivos

- Calendário de feriados no cálculo de dias úteis.
- Dados fictícios no backend. O seed fica no frontend (`npm run seed`), conforme `auditoria-restaurante-e-conflitos`; a atualização dele é tarefa da change do front.
- Recalcular o `total_price` persistido da obra a cada mutação de refeição ou hospedagem (escopo de `endurecer-dominio-e-persistencia`). A API devolve o total calculado no domínio na leitura.

## Capabilities

### New Capabilities

- `project-meal-plan`: alimentação contratada de cada obra, com tipos de refeição, quantidade e preço por refeição servida, custo por dia, dias úteis e total estimado, e uma cobertura de almoço e uma de jantar por obra.
- `optional-contact-email`: e-mail opcional, validado e único quando informado, nos cadastros de cliente e de colaborador.

### Modified Capabilities

(nenhuma; `openspec/specs/` ainda está vazio. `meal-assignment-uniqueness`, de `deploy-viavel`, e `project-cost-totalization`, de `endurecer-dominio-e-persistencia`, não foram arquivadas. Esta change substitui a regra "uma refeição de cada tipo por obra" pela regra de cobertura e o total da obra por refeição pelo total estimado; o ajuste é registrado no design.)

## Impact

**Código**
- `core/domain`: `Meal` (tipo, campos por refeição servida, custos calculados, sobreposição de cobertura), `Project` (lista de refeições, dias úteis, total), `Client` e `Employee` (e-mail opcional).
- `core/exception`: `InvalidMealException` com os novos códigos.
- `core/repository`: `MealRepository` sem consultas por data.
- `application`: DTOs, mappers e casos de uso de refeição, obra, cliente e colaborador.
- `infra`: `MealEntity`, `ClientEntity`, `EmployeeEntity`, mappers e adaptadores, `IntegrityViolationTranslator`, documentação OpenAPI dos controllers.
- `src/main/resources/db/migration/V4__refeicoes_por_obra_e_email_opcional.sql`.

**API**
- **BREAKING**: `POST/PUT /api/meals`, respostas de refeição e de obra, `ProjectRequestDTO`.
- E-mail de cliente e colaborador pode vir `null`.

**Documentação**
- `README.md` (contrato de refeição e BREAKING) e `app-adm-obras-erd.puml`.

**Integração**
- O frontend publica a parte de refeições junto com esta change. A `V4` não tem reversão automática.
