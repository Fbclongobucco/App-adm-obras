## Purpose

Define como o custo total de um projeto é derivado de suas refeições e acomodações, e garantir que o valor persistido e devolvido pela API reflita sempre os componentes atuais do projeto, independentemente de qual caminho de mutação foi exercido.

## ADDED Requirements

### Requirement: Project total is the sum of its current components

O custo total de um projeto SHALL ser igual à soma do total da refeição de almoço, do total da refeição de janta e dos totais de todas as acomodações atualmente vinculadas ao projeto. O valor persistido no projeto SHALL ser o valor devolvido pela API.

#### Scenario: Total reflects a project with both meals and accommodations

- **WHEN** um projeto possui uma refeição de almoço, uma de janta e duas acomodações vinculadas
- **THEN** o total devolvido do projeto é exatamente a soma dos totais dessas quatro entidades
- **AND** o total persistido é idêntico ao devolvido

#### Scenario: Project with no components totals zero

- **WHEN** um projeto não possui refeições nem acomodações vinculadas
- **THEN** o total devolvido é zero

### Requirement: Every mutation path refreshes the total

Qualquer operação que altere os componentes de custo de um projeto SHALL refletir no total persistido do projeto. Isso SHALL ocorrer para criação, alteração e exclusão de refeições e de acomodações, e para operações que transferem uma refeição ou acomodação entre projetos. O recálculo SHALL acontecer na mesma transação da mutação que o invalidou.

#### Scenario: Creating a meal updates the project total

- **WHEN** uma refeição é criada e vinculada a um projeto existente
- **THEN** o total do projeto passa a incluir o total daquela refeição

#### Scenario: Updating a meal price updates the project total

- **WHEN** o preço de uma refeição vinculada a um projeto é alterado
- **THEN** o total do projeto reflete o novo preço multiplicado pela quantidade vigente

#### Scenario: Updating a meal quantity updates the project total

- **WHEN** a quantidade de uma refeição vinculada a um projeto é alterada
- **THEN** o total do projeto reflete o preço vigente multiplicado pela nova quantidade

#### Scenario: Deleting a meal removes it from the project total

- **WHEN** uma refeição vinculada a um projeto é excluída
- **THEN** o total do projeto deixa de incluir o total daquela refeição

#### Scenario: Creating an accommodation updates the project total

- **WHEN** uma acomodação é criada e vinculada a um projeto existente
- **THEN** o total do projeto passa a incluir o total daquela acomodação

#### Scenario: Updating an accommodation total updates the project total

- **WHEN** o total informado de uma acomodação vinculada a um projeto é alterado
- **THEN** o total do projeto reflete o novo valor da acomodação

#### Scenario: Deleting an accommodation removes it from the project total

- **WHEN** uma acomodação vinculada a um projeto é excluída
- **THEN** o total do projeto deixa de incluir o total daquela acomodação

#### Scenario: Replacing the project's meals through the project endpoint refreshes the total

- **WHEN** a lista de refeições de um projeto é substituída pela alteração do próprio projeto
- **THEN** o total do projeto corresponde exatamente às refeições agora vinculadas

### Requirement: Moving a component refreshes both affected projects

Quando uma refeição ou acomodação é transferida de um projeto para outro, o total dos dois projetos afetados SHALL refletir a transferência.

#### Scenario: Moving a meal between projects updates both totals

- **WHEN** uma refeição vinculada ao projeto A passa a estar vinculada ao projeto B
- **THEN** o total do projeto A deixa de incluir aquela refeição
- **AND** o total do projeto B passa a incluir aquela refeição

### Requirement: Failed mutations never leave a divergent total

Se uma mutação de componente for rejeitada, o total persistido do projeto SHALL permanecer inalterado. O recálculo do total SHALL ser atômico com a mutação.

#### Scenario: Rejected meal update leaves the total untouched

- **WHEN** uma alteração de refeição é rejeitada por violar uma regra de validação
- **THEN** nem a refeição nem o total do projeto são alterados

#### Scenario: Transaction rollback discards the recomputed total

- **WHEN** uma transação que inclui o recálculo do total falha após o recálculo e antes da confirmação
- **THEN** o total persistido do projeto permanece com o valor anterior

### Requirement: Component totals declare their origin and are validated consistently

O total de uma refeição SHALL ser sempre derivado do preço unitário pela quantidade, e não aceito como entrada. O total de uma acomodação SHALL ser sempre um valor informado, validado como não-negativo, e não calculado a partir de outros campos. Um valor negativo em qualquer total de componente SHALL ser rejeitado com erro de validação. O sistema SHALL tratar os dois tipos de total como contributions igualmente válidas para a soma, sem pressupor que tenham a mesma origem.

#### Scenario: Meal total is always recomputed from price and quantity

- **WHEN** uma refeição é lida com preço e quantidade definidos
- **THEN** o total devolvido é o produto desses dois valores

#### Scenario: Accommodation total is accepted as a reported figure

- **WHEN** uma acomodação é criada com um total informado não-negativo
- **THEN** o total informado é preservado, sem ser recalculado a partir de outros campos da acomodação

#### Scenario: Negative component total is rejected

- **WHEN** uma criação ou alteração resulta em total de componente negativo
- **THEN** a requisição é rejeitada com erro de validação
- **AND** nenhum dado é persistido