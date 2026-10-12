## Purpose

Publicar uma descrição legível por máquina e uma interface interativa da API HTTP, geradas a partir do código, para apoiar integração e testes manuais nos ambientes de desenvolvimento e teste sem expor o mapa da API em produção.

## ADDED Requirements

### Requirement: API description is published in OpenAPI format

Onde a documentação estiver ativa, o sistema SHALL publicar uma descrição da API no formato OpenAPI 3, acessível por HTTP, que inclua toda rota sob `/api`, com método, caminho, parâmetros e estrutura de requisição e resposta.

#### Scenario: Description is served as OpenAPI JSON

- **WHEN** um cliente requisita o caminho da descrição da API em um ambiente com documentação ativa
- **THEN** a resposta é bem-sucedida e o corpo é um documento OpenAPI 3 válido

#### Scenario: Every API route is described

- **WHEN** as rotas declaradas pela aplicação sob `/api` são comparadas com as operações da descrição publicada
- **THEN** toda rota existente aparece na descrição com o mesmo método e caminho

### Requirement: API description is derived from the code

A descrição publicada SHALL ser gerada a partir das rotas e tipos de dados da própria aplicação no momento da execução, e SHALL NOT depender de arquivo de descrição mantido à mão.

#### Scenario: New route appears without manual documentation

- **WHEN** uma nova rota é adicionada sob `/api` e a aplicação é reiniciada
- **THEN** a rota aparece na descrição publicada sem edição de arquivo de documentação

### Requirement: Interactive documentation interface is served

Onde a documentação estiver ativa, o sistema SHALL servir uma interface web que apresenta a descrição da API e permite montar e enviar requisições às rotas descritas.

#### Scenario: Interface loads in the browser

- **WHEN** um usuário acessa o caminho da interface em um ambiente com documentação ativa
- **THEN** a interface é exibida e lista as rotas da API agrupadas por recurso

### Requirement: Documentation paths require no authentication

Onde a documentação estiver ativa, a descrição da API e a interface SHALL ser acessíveis sem credenciais.

#### Scenario: Anonymous access to the description

- **WHEN** um cliente sem credenciais requisita a descrição da API em um ambiente com documentação ativa
- **THEN** a resposta é bem-sucedida, sem desafio de autenticação

#### Scenario: Anonymous access to the interface

- **WHEN** um cliente sem credenciais acessa a interface em um ambiente com documentação ativa
- **THEN** a interface é entregue, sem desafio de autenticação

### Requirement: Requests sent from the interface follow existing authorization

A descrição SHALL declarar o esquema de autenticação HTTP Basic aceito pela API, e a interface SHALL permitir informar credenciais. Requisições enviadas pela interface SHALL estar sujeitas às mesmas regras de autenticação e autorização de qualquer outro cliente.

#### Scenario: Request without credentials is refused

- **WHEN** uma rota protegida é executada pela interface sem credenciais
- **THEN** a API responde com recusa de autenticação, como responderia a qualquer cliente

#### Scenario: Request with insufficient role is refused

- **WHEN** um usuário com perfil `OPERADOR` executa pela interface uma operação de escrita restrita a `ADMIN`
- **THEN** a API responde com recusa de autorização

#### Scenario: Request with valid credentials succeeds

- **WHEN** um usuário `ADMIN` informa suas credenciais na interface e executa uma rota de leitura
- **THEN** a API responde com sucesso

### Requirement: Documentation is not served in production

No perfil de produção, o sistema SHALL NOT servir a descrição da API nem a interface, com ou sem credenciais.

#### Scenario: Description is unavailable in production

- **WHEN** um cliente, autenticado ou não, requisita o caminho da descrição da API com o perfil de produção ativo
- **THEN** nenhuma descrição da API é retornada

#### Scenario: Interface is unavailable in production

- **WHEN** um cliente, autenticado ou não, acessa o caminho da interface com o perfil de produção ativo
- **THEN** a interface não é entregue

### Requirement: Documentation is disabled unless a profile enables it

A documentação SHALL ficar desativada por padrão e ativa apenas nos perfis que a habilitam explicitamente: o de desenvolvimento local e o de teste.

#### Scenario: Development and test profiles serve documentation

- **WHEN** a aplicação sobe com o perfil de desenvolvimento local ou com o perfil de teste
- **THEN** a descrição da API e a interface estão disponíveis

#### Scenario: Unlisted profile does not serve documentation

- **WHEN** a aplicação sobe com um perfil que não habilita a documentação
- **THEN** a descrição da API e a interface não estão disponíveis
