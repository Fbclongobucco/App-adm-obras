## Why

A API tem 55 rotas em 9 controllers sob `/api`, e não existe nenhuma descrição dela fora do código-fonte. Quem integra o frontend ou testa manualmente precisa ler os controllers e DTOs, ou montar requisições à mão com HTTP Basic. Uma descrição OpenAPI gerada a partir do código, com interface interativa (Swagger UI), resolve isso sem documentação paralela que desatualize.

## What Changes

- A aplicação passa a publicar a descrição OpenAPI 3 das rotas `/api/**`, gerada a partir dos controllers e DTOs existentes.
- A aplicação passa a servir o Swagger UI, com suporte a autenticação HTTP Basic para executar requisições pela própria interface.
- Documentação e interface ficam **ativas apenas nos perfis `local` e `test`**. No perfil `prod`, e em qualquer perfil que não as habilite explicitamente, os endpoints não existem.
- Os caminhos da documentação são acessíveis sem autenticação onde estiverem ativos. As rotas `/api/**` executadas pela interface seguem as mesmas regras de autorização de hoje (`ADMIN`/`OPERADOR`).
- Nova dependência: `springdoc-openapi-starter-webmvc-ui`.

### Não-objetivos

- Anotar manualmente cada endpoint com descrições, exemplos ou códigos de resposta. A primeira versão usa só o que é inferido do código; o enriquecimento fica para depois, por demanda.
- Publicar a documentação em produção, com ou sem autenticação.
- Gerar cliente de API a partir da especificação.
- Alterar contrato, rota ou regra de autorização de qualquer endpoint de negócio.

## Capabilities

### New Capabilities

- `api-documentation`: descrição OpenAPI e interface interativa da API HTTP, gerada a partir do código e disponível apenas nos ambientes de desenvolvimento e teste.

### Modified Capabilities

(nenhuma; `openspec/specs/` ainda está vazio.)

## Impact

**Dependências**
- `pom.xml`: `+org.springdoc:springdoc-openapi-starter-webmvc-ui`, com versão explícita, já que não é gerenciada pelo parent do Spring Boot.

**Configuração**
- `application.properties`: documentação desligada por padrão.
- `application-local.properties`, `application-test.properties`: documentação ligada.
- `application-prod.properties`: desligamento explícito.

**Código**
- `infra/security/SecurityConfig.java`: liberação sem autenticação dos caminhos da documentação.
- Nova classe de configuração em `infra/config/` com metadados da API (título, versão) e o esquema de segurança HTTP Basic.

**API**
- Novos caminhos `/v3/api-docs/**` e `/swagger-ui/**` (mais `/swagger-ui.html`) nos perfis `local` e `test`. Em `prod`, nenhum caminho novo.
- Nenhuma rota de negócio muda.

**Documentação**
- `README.md`: como acessar o Swagger UI e autenticar.

**Relação com outros changes**
- Independente de `deploy-viavel` e de `endurecer-dominio-e-persistencia`. Pode ser aplicado antes ou depois deles.
