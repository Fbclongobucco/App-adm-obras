## Purpose

Representa os restaurantes que atendem cada obra como cadastro próprio, vinculado obrigatoriamente a uma obra, e faz as refeições referenciarem esse cadastro em vez de repetir o nome do restaurante.

## ADDED Requirements

### Requirement: Restaurant requires a project

Um restaurante SHALL pertencer a exatamente uma obra existente. Criação ou alteração sem `projectId` SHALL ser rejeitada com 400 e `errorCode` `restaurant.invalid.project`; obra inexistente SHALL responder 404 `project.not_found`.

#### Scenario: Restaurant created for an existing project

- **WHEN** um restaurante é criado com nome válido e `projectId` de uma obra existente
- **THEN** a resposta traz o restaurante com a obra vinculada

#### Scenario: Restaurant without project is rejected

- **WHEN** um restaurante é criado sem `projectId`
- **THEN** a resposta é 400 com `errorCode` `restaurant.invalid.project`
- **AND** nenhum restaurante é gravado

### Requirement: Restaurant fields are validated

O nome SHALL ter de 2 a 100 caracteres após normalização de espaços. O telefone, quando informado, SHALL ter 10 ou 11 dígitos. O endereço, quando informado, SHALL seguir as mesmas regras de endereço dos demais cadastros.

#### Scenario: Short name is rejected

- **WHEN** um restaurante é criado com nome de 1 caractere
- **THEN** a resposta é 400 com `errorCode` `restaurant.invalid.name`

#### Scenario: Optional phone and address

- **WHEN** um restaurante é criado sem telefone e sem endereço
- **THEN** o restaurante é gravado com os dois campos vazios

### Requirement: Restaurants are listed per project

O sistema SHALL listar os restaurantes de forma paginada e SHALL listar os restaurantes de uma obra específica.

#### Scenario: Listing restaurants of a project

- **WHEN** os restaurantes de uma obra são pedidos
- **THEN** a resposta traz apenas os restaurantes vinculados a essa obra

### Requirement: Meal references a restaurant

Uma refeição SHALL referenciar um restaurante existente por `restaurantId`, obrigatório. A obra da refeição SHALL ser a obra do restaurante. A requisição de refeição SHALL NOT aceitar nome de restaurante, endereço nem obra.

#### Scenario: Meal inherits the restaurant's project

- **WHEN** uma refeição é criada para um restaurante da obra A
- **THEN** a resposta traz o restaurante e a obra A

#### Scenario: Meal without restaurant is rejected

- **WHEN** uma refeição é criada sem `restaurantId`
- **THEN** a resposta é 400 com `errorCode` `meal.invalid.restaurant`

#### Scenario: Unknown restaurant

- **WHEN** uma refeição é criada com `restaurantId` inexistente
- **THEN** a resposta é 404 com `errorCode` `restaurant.not_found`

### Requirement: One meal of each type per project still holds

A regra de no máximo uma refeição de cada tipo por obra SHALL ser avaliada sobre a obra do restaurante. A violação SHALL responder 409 com `errorCode` `meal.duplicate.type`.

#### Scenario: Second lunch through another restaurant of the same project

- **WHEN** a obra A já tem almoço pelo restaurante R1 e um almoço é criado pelo restaurante R2, também da obra A
- **THEN** a resposta é 409 com `errorCode` `meal.duplicate.type`

### Requirement: Restaurant with meals keeps its project

Um restaurante com refeições SHALL NOT mudar de obra. A tentativa SHALL responder 409 com `errorCode` `restaurant.in_use`, sem alterar dados.

#### Scenario: Moving a restaurant with meals is blocked

- **WHEN** um restaurante com refeições é alterado para outra obra
- **THEN** a resposta é 409 com `errorCode` `restaurant.in_use`
- **AND** o restaurante continua na obra original

#### Scenario: Moving a restaurant without meals is allowed

- **WHEN** um restaurante sem refeições é alterado para outra obra
- **THEN** a alteração é aceita

### Requirement: Existing meals are migrated to restaurants

Na migração, cada par distinto (obra, nome de restaurante) das refeições existentes SHALL virar um restaurante daquela obra, com o endereço de uma das refeições do par, e cada refeição SHALL ficar ligada ao seu restaurante.

#### Scenario: Two meals with the same restaurant name in one project

- **WHEN** a obra A tem almoço e janta com o mesmo nome de restaurante antes da migração
- **THEN** após a migração existe um único restaurante com esse nome na obra A, referenciado pelas duas refeições

### Requirement: Project update does not move meals

No PUT de obra, `lunchId` e `dinnerId` SHALL apenas ser validados: a refeição SHALL existir, ter o tipo do campo e pertencer à obra pelo restaurante; senão, 400 `project.invalid.meal`. Omitir os campos SHALL NOT desvincular refeições.

#### Scenario: Meal from another project is rejected

- **WHEN** uma obra A é alterada com `lunchId` de uma refeição da obra B
- **THEN** a resposta é 400 com `errorCode` `project.invalid.meal`
- **AND** a refeição continua na obra B

#### Scenario: Omitting meal ids keeps meals

- **WHEN** uma obra com almoço e janta é alterada sem `lunchId` e sem `dinnerId`
- **THEN** a leitura da obra continua trazendo o almoço e a janta
