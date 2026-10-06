## Purpose

Definir como a configuração da aplicação é fornecida ao processo em execução, como ela é documentada sem expor segredos, e como a ausência de configuração obrigatória é tratada.

## ADDED Requirements

### Requirement: Configuration is supplied by environment variables

A configuração de ambiente da aplicação SHALL ser fornecida por variáveis de ambiente. O sistema SHALL documentar, em um arquivo de exemplo versionado junto ao projeto, toda variável de ambiente que a aplicação efetivamente lê, cada uma acompanhada de valor de exemplo e de descrição do que configura.

#### Scenario: Every consumed variable appears in the example file

- **WHEN** o arquivo de exemplo é comparado com as variáveis que a aplicação resolve da configuração
- **THEN** toda variável lida pela aplicação está presente no arquivo de exemplo
- **AND** o arquivo de exemplo não documenta variável que a aplicação não leia

#### Scenario: Example file is usable as a starting point

- **WHEN** alguém copia o arquivo de exemplo para um arquivo local e preenche os valores
- **THEN** a aplicação sobe com essa configuração sem exigir ajuste adicional fora do arquivo

#### Scenario: Example file carries no real secret

- **WHEN** o arquivo de exemplo é inspecionado
- **THEN** os valores presentes são valores de exemplo, e não credenciais reais

### Requirement: Real environment files stay out of version control

O arquivo de ambiente real, com os valores de uma instalação, SHALL NOT ser versionado. O arquivo de exemplo SHALL ser versionado.

#### Scenario: Local environment file is ignored

- **WHEN** um arquivo de ambiente real é criado no diretório do projeto
- **THEN** o Git o reporta como ignorado

#### Scenario: Example file is tracked

- **WHEN** o status do repositório é consultado
- **THEN** o arquivo de exemplo aparece como versionável e o arquivo real não aparece

#### Scenario: Secrets are not required to be committed for setup

- **WHEN** um novo desenvolvedor clona o repositório
- **THEN** ele obtém o arquivo de exemplo versionado e não obtém as credenciais de nenhuma instalação

### Requirement: Missing mandatory configuration fails at startup

Quando uma variável de ambiente obrigatória estiver ausente ou inválida, a aplicação SHALL falhar na inicialização, identificando a variável em questão. A aplicação SHALL NOT subir em estado parcialmente configurado e falhar apenas na primeira requisição que precisar do dado faltante.

#### Scenario: Missing database host is reported by name

- **WHEN** a aplicação sobe sem a variável que define o endereço do banco
- **THEN** a inicialização falha
- **AND** a mensagem de erro identifica a variável ausente

#### Scenario: Missing database credentials are reported by name

- **WHEN** a aplicação sobe sem usuário ou sem senha de banco
- **THEN** a inicialização falha identificando a variável ausente

#### Scenario: Failure precedes any request handling

- **WHEN** a inicialização falha por configuração incompleta
- **THEN** a aplicação não passa a atender requisições

#### Scenario: Invalid value is rejected rather than silently defaulted

- **WHEN** uma variável obrigatória é fornecida com valor que não pode ser interpretado
- **THEN** a inicialização falha, em vez de assumir silenciosamente um valor padrão

### Requirement: Optional configuration is distinguishable from mandatory

O arquivo de exemplo SHALL distinguir, por meio de comentários, quais variáveis são obrigatórias para a aplicação subir e quais são opcionais com valor padrão. Uma variável opcional ausente SHALL NOT impedir a inicialização.

#### Scenario: Optional variable may be omitted

- **WHEN** a aplicação sobe sem uma variável marcada como opcional no exemplo
- **THEN** ela sobe usando o valor padrão documentado

#### Scenario: Mandatory variable cannot be omitted

- **WHEN** a aplicação sobe sem uma variável marcada como obrigatória no exemplo
- **THEN** a inicialização falha

### Requirement: Defaults documented in the example are real

Todo valor padrão declarado no arquivo de exemplo SHALL corresponder ao valor padrão efetivamente assumido pela aplicação quando a variável está ausente.

#### Scenario: Documented default matches actual default

- **WHEN** a aplicação sobe com uma variável opcional ausente
- **THEN** o valor assumido é igual ao valor default declarado no arquivo de exemplo