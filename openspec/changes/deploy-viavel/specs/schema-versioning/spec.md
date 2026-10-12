## Purpose

Define que o schema do banco de dados é propriedade de migrações versionadas e controladas, aplicadas de forma determinística na inicialização da aplicação, com um único conjunto de migrações válido tanto no ambiente de desenvolvimento quanto em produção.

## ADDED Requirements

### Requirement: Schema is created and evolved only by versioned migrations

O schema do banco de dados SHALL ser criado e alterado exclusivamente por migrações versionadas aplicadas pelo sistema de migração na inicialização da aplicação. Nenhum perfil SHALL criar ou alterar tabelas por mecanismo automático de mapeamento objeto-relacional.

#### Scenario: Empty database is provisioned on startup

- **WHEN** a aplicação é iniciada contra um banco vazio
- **THEN** as migrações pendentes são aplicadas e todas as tabelas e índices necessários existem ao final

#### Scenario: No profile mutates the schema automatically

- **WHEN** a aplicação é iniciada em qualquer perfil
- **THEN** a criação e alteração de schema vem exclusivamente das migrações versionadas

### Requirement: Schema changes are expressed as ordered, versioned files

Cada alteração de schema SHALL ser registrada como um arquivo de migração versionado e ordenado, de modo que o estado do schema seja reproduzível a partir do repositório e da série de versões aplicada. Nenhuma alteração de schema SHALL depender de edição manual do banco.

#### Scenario: Schema state is reproducible from migrations alone

- **WHEN** um banco é criado do zero e a aplicação é iniciada
- **THEN** o schema resultante é equivalente ao de um banco onde a mesma série de migrações foi aplicada passo a passo

#### Scenario: Migration set is reviewed as source

- **WHEN** o schema de um ambiente é inspecionado
- **THEN** é possível identificar, a partir dos arquivos versionados do repositório, qual versão o schema representa

### Requirement: One migration set is valid across development and production databases

O mesmo conjunto de arquivos de migração SHALL ser aplicável sem alteração ao banco de desenvolvimento e de testes e ao banco de produção, apesar de serem motores distintos. Nenhum passo de migração SHALL exigir SQL específico de um motor com equivalente não portável em outro.

#### Scenario: Migrations apply on both database engines unchanged

- **WHEN** o mesmo conjunto de migrações é aplicado no banco de desenvolvimento e no banco de produção
- **THEN** ambos terminam com sucesso e com schema equivalente

#### Scenario: Development migrations need no engine-specific step

- **WHEN** uma migração é adicionada ao conjunto
- **THEN** ela é aplicada sem edição específica por ambiente

### Requirement: Startup fails loudly on schema inconsistency

A aplicação SHALL recusar iniciar quando o estado do schema do banco for inconsistente com o conjunto de migrações, em vez de operar sobre um schema divergente. A recusa SHALL ocorrer antes de qualquer decisão de negócio ser atendida.

#### Scenario: Pending migration blocks serving requests

- **WHEN** existe migração pendente e a aplicação é iniciada
- **THEN** a aplicação aplica as migrações pendentes antes de atender requisições

#### Scenario: Unknown schema version blocks startup

- **WHEN** o banco registra uma versão de schema que o código não conhece
- **THEN** a aplicação falha na inicialização e não atende requisições

### Requirement: Repeat startups are safe

Executar a inicialização repetidamente sobre o mesmo banco SHALL ser seguro e não SHALL produzir efeito adicional.

#### Scenario: Second startup applies nothing new

- **WHEN** a aplicação é iniciada contra um banco já atualizado
- **THEN** nenhuma migração é reaplicada e a aplicação inicia normalmente

#### Scenario: Data is preserved across restarts

- **WHEN** a aplicação é reiniciada contra um banco com dados
- **THEN** os dados existentes permanecem intactos após a inicialização