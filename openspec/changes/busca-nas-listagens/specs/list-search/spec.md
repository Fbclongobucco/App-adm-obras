## ADDED Requirements

### Requirement: Parâmetro de busca nas listagens
As listagens paginadas de obras, clientes, colaboradores, centros de custo, restaurantes, refeições, hospedagens e usuários SHALL aceitar o parâmetro opcional `q`. Sem `q`, ou com `q` só de espaços, o resultado MUST ser o mesmo de antes. Com `q`, MUST retornar só os registros em que algum campo pesquisável contém o termo, ignorando maiúsculas, minúsculas e acentos. Paginação e `totalElements` MUST refletir o resultado filtrado.

#### Scenario: Sem acento
- **WHEN** existe o colaborador "José Antônio" e a API recebe `GET /api/employees?q=jose antonio`
- **THEN** o colaborador aparece no resultado

#### Scenario: Total filtrado
- **WHEN** 3 de 10 clientes contêm "campo" no nome e a API recebe `GET /api/clients?q=campo`
- **THEN** `totalElements` é 3

### Requirement: Termo inválido
Termos com mais de 100 caracteres SHALL ser rejeitados com 400 e errorCode `page.invalid.query`.

#### Scenario: Termo longo
- **WHEN** a API recebe `q` com 101 caracteres
- **THEN** responde 400 com errorCode `page.invalid.query`

### Requirement: Campos pesquisáveis
Cada listagem SHALL pesquisar: obras (OS, descrição, nome do cliente); clientes (nome, e-mail, cidade); colaboradores (nome, CPF, e-mail); centros de custo (nome, CNPJ); restaurantes (nome, OS da obra, cidade); refeições (nome do restaurante, OS da obra); hospedagens (anfitrião, cidade, OS da obra); usuários (nome, e-mail). CPF e CNPJ MUST ser comparados só pelos dígitos.

#### Scenario: CPF formatado
- **WHEN** um colaborador tem CPF 52998224725 e a API recebe `q=529.982`
- **THEN** o colaborador aparece no resultado

#### Scenario: Campo de relação
- **WHEN** a obra é do cliente "Agroindustrial Campo Novo" e a API recebe `GET /api/projects?q=campo novo`
- **THEN** a obra aparece no resultado

#### Scenario: Registro sem endereço
- **WHEN** um restaurante sem endereço tem o nome "Cantina Boa Mesa" e a API recebe `GET /api/restaurants?q=boa mesa`
- **THEN** o restaurante aparece no resultado
