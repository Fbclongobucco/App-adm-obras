## Purpose

Garante que mudar o centro de custo de um colaborador apenas mova o colaborador, sem alterar nenhum centro de custo cadastrado.

## ADDED Requirements

### Requirement: Changing cost center moves the employee

Alterar `costCenterId` de um colaborador SHALL vinculá-lo ao centro de custo informado. Leituras posteriores do colaborador e as listagens por centro de custo SHALL refletir o novo vínculo.

#### Scenario: Employee moves to another cost center

- **WHEN** um colaborador do centro A é alterado com `costCenterId` do centro B
- **THEN** a leitura do colaborador traz o centro B
- **AND** o colaborador aparece na listagem do centro B e não na do centro A

### Requirement: Cost centers are not modified by employee updates

Nenhuma alteração de colaborador SHALL modificar nome ou CNPJ de qualquer centro de custo.

#### Scenario: Previous cost center keeps its data

- **WHEN** um colaborador do centro A é movido para o centro B
- **THEN** o centro A mantém nome e CNPJ originais
- **AND** o centro B mantém nome e CNPJ originais

### Requirement: Unknown cost center is rejected

Alteração de colaborador com `costCenterId` inexistente SHALL responder 404 com `errorCode` `cost_center.not_found`, sem alterar o colaborador.

#### Scenario: Move to a missing cost center

- **WHEN** um colaborador é alterado com `costCenterId` inexistente
- **THEN** a resposta é 404 com `errorCode` `cost_center.not_found`
- **AND** o colaborador continua no centro anterior
