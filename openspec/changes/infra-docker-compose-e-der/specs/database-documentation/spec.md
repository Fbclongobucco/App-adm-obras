## Purpose

Garantir que o schema do banco de dados esteja documentado por um diagrama entidade-relacionamento versionado em PDF, gerado a partir de fonte textual revisável, e que exista um caminho explícito para mantê-lo em dia quando o schema mudar.

## ADDED Requirements

### Requirement: A committed PDF ER diagram exists at the repository root

O repositório SHALL conter, na raiz, um diagrama entidade-relacionamento em PDF que descreva o schema do banco. O arquivo SHALL ser versionado, de modo que quem não tenha a ferramenta de geração o obtenha ao clonar o repositório.

#### Scenario: PDF is present after cloning

- **WHEN** o repositório é clonado
- **THEN** o PDF do diagrama está presente na raiz, sem nenhum passo de geração necessário

#### Scenario: PDF is a valid readable document

- **WHEN** o PDF é aberto
- **THEN** ele é um documento válido e exibe o diagrama

### Requirement: The diagram is generated from a reviewable textual source

O PDF SHALL ser produzido a partir de um arquivo de texto versionado que descreve as entidades e relacionamentos. Alterações no diagrama SHALL ser feitas no texto, e o PDF regenerado a partir dele. O PDF SHALL NOT ser o único artefato e SHALL NOT ser editado manualmente como fonte da verdade.

#### Scenario: Source text is reviewable in the repository

- **WHEN** o repositório é inspecionado
- **THEN** existe um arquivo de texto, em caminho previsível, que descreve as entidades e os relacionamentos

#### Scenario: Source change regenerates the PDF

- **WHEN** o arquivo de texto é alterado e a geração é executada
- **THEN** o PDF resultante reflete a alteração

#### Scenario: Diffing a diagram change is possible

- **WHEN** o diagrama é alterado
- **THEN** a alteração é revisável como texto no histórico, sem exigir comparação visual de binários

### Requirement: Generation is reproducible without system-installed tooling

A geração do PDF SHALL ser executável a partir de um comando documentado no projeto, e SHALL NOT exigir instalação de ferramenta de sistema. Um ambiente limpo com as dependências do projeto declaradas SHALL ser suficiente para gerar o PDF.

#### Scenario: Generation works from the documented command

- **WHEN** o comando de geração documentado é executado em um ambiente limpo
- **THEN** o PDF é gerado sem erro

#### Scenario: No system package installation required

- **WHEN** a geração é executada
- **THEN** nenhuma ferramenta de sistema precisa estar instalada previamente

#### Scenario: Repeated generation is stable

- **WHEN** a geração é executada duas vezes a partir da mesma fonte
- **THEN** o PDF resultante representa o mesmo diagrama

### Requirement: The diagram represents the schema as it is

O diagrama SHALL representar o schema vigente, incluindo todas as tabelas, suas colunas, chaves primárias, chaves estrangeiras, cardinalidades dos relacionamentos, restrições de unicidade, tabelas de junção de relacionamentos muitos-para-muitos e tabelas de coleção de elemento.

#### Scenario: Every table appears

- **WHEN** o diagrama é comparado com as entidades mapeadas
- **THEN** toda tabela do schema aparece no diagrama

#### Scenario: Relationships carry cardinality

- **WHEN** o diagrama é inspecionado
- **THEN** cada relacionamento entre tabelas está apresentado com cardinalidade identificável

#### Scenario: Join tables appear as relationships

- **WHEN** o diagrama é inspecionado
- **THEN** os relacionamentos muitos-para-muitos aparecem na forma de tabela de junção, com suas colunas

#### Scenario: Element collections appear

- **WHEN** o diagrama é inspecionado
- **THEN** as coleções de elemento, mantidas em tabela própria, aparecem no diagrama

#### Scenario: Unique constraints are visible

- **WHEN** o diagrama é inspecionado
- **THEN** as colunas sujeitas a restrição de unicidade estão identificadas como tal

### Requirement: The diagram declares which schema state it reflects

O diagrama SHALL indicar qual estado do schema ele representa, de modo que não possa ser lido como descrição de um schema que ainda não existe.

#### Scenario: Provenance is stated on the diagram

- **WHEN** o diagrama é aberto
- **THEN** ele informa a origem e o momento do schema que representa

### Requirement: A path to regenerate exists when the schema changes

Quando o schema do banco mudar, o diagrama SHALL ser regenerado na mesma alteração. O diagrama atualizado SHALL acompanhar a alteração que mudou o schema.

#### Scenario: Schema change includes regenerated diagram

- **WHEN** uma alteração de schema é feita
- **THEN** o PDF do diagrama é regenerado e versionado na mesma alteração

#### Scenario: Diagram source is the reviewed artifact

- **WHEN** uma alteração de schema é revisada
- **THEN** a alteração inclui o arquivo de texto do diagrama junto da mudança de schema, e não apenas o PDF binário

#### Scenario: Source of truth is unambiguous

- **WHEN** o diagrama é alterado
- **THEN** a alteração foi feita no arquivo de texto, e o PDF é consequência disso

#### Scenario: Diagram regeneration does not depend on a running database

- **WHEN** o diagrama é regenerado
- **THEN** não é necessário que um banco de dados esteja disponível ou em execução