# Tasks

## 1. Dependência e ativação por perfil

- [x] 1.1 Adicionar `org.springdoc:springdoc-openapi-starter-webmvc-ui` com versão explícita `3.1.1` ao `pom.xml`. Verificar com `./mvnw dependency:tree` que a dependência resolve e que nenhum artefato do Spring Boot é trazido em versão diferente de `4.1.1`.
- [x] 1.2 Rodar `./mvnw test` com a dependência adicionada e ainda sem nenhuma configuração. A suíte deve continuar verde, confirmando a compatibilidade com Spring Boot 4.1.1. Em caso de falha, trocar a versão do springdoc antes de seguir (design D1).
- [x] 1.3 Definir `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` em `application.properties`, os dois `true` em `application-local.properties` e `application-test.properties`, e os dois `false` explícitos em `application-prod.properties`. Verificar por grep que os quatro arquivos têm as duas chaves com os valores previstos.

## 2. Segurança, metadados e testes

- [x] 2.1 Em `SecurityConfig`, adicionar `requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()` antes de `anyRequest()`, sem alterar as regras de `/api/**`. Verificar que `SecurityConfigTest` continua verde.
- [x] 2.2 Criar `infra/config/OpenApiConfig.java` com o bean `OpenAPI`: título `app-adm-obras API`, versão do artefato, esquema `basicAuth` (http/basic) e requisito de segurança global. Verificar que `core/` e `application/` continuam sem import de `org.springdoc` ou `io.swagger`.
- [x] 2.3 Criar teste de integração `@SpringBootTest(RANDOM_PORT)` com perfil `test`, no padrão de `AuthControllerIntegrationTest`, cobrindo:
  - `GET /v3/api-docs` sem credenciais responde 200 com `openapi` iniciando em `3.`;
  - toda rota mapeada sob `/api`, obtida de `RequestMappingHandlerMapping`, aparece em `paths` com o mesmo método;
  - `components.securitySchemes.basicAuth` existe com `scheme: basic`;
  - `GET /swagger-ui/index.html` sem credenciais responde 200 com HTML;
  - `GET /api/clients` sem credenciais continua respondendo 401.
- [x] 2.4 Criar teste de integração com perfil `test` e as propriedades `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` sobrescritas. `GET /v3/api-docs` e `GET /swagger-ui/index.html` não devem entregar descrição nem interface: 404 sem credenciais e com credenciais ADMIN (design D2/D3).
- [x] 2.5 Criar teste que carrega `application.properties` e `application-prod.properties` como recursos e verifica que as duas chaves `springdoc.*.enabled` valem `false`. O perfil `prod` exige PostgreSQL e não sobe nos testes, então este teste fixa o requisito "não servido em produção" sem banco.
- [x] 2.6 Rodar `./mvnw test` completo e confirmar a suíte verde.

## 3. Documentação e verificação manual

- [x] 3.1 Adicionar ao `README.md` a seção "API documentation": URL do Swagger UI (`http://localhost:8080/swagger-ui.html`), URL da descrição (`/v3/api-docs`), perfis onde está ativa, aviso de que não existe em `prod` e como usar "Authorize" com HTTP Basic. Verificar que as URLs do README abrem com a aplicação rodando em `local`.
- [ ] 3.2 Verificação manual em `local` com `APP_BOOTSTRAP_ENABLED=true`:
  - a UI lista as rotas agrupadas por controller;
  - sem "Authorize", `GET /api/clients` pela UI dá 401;
  - com as credenciais do admin, dá 200.
  
  Registrar o resultado na revisão.
- [x] 3.3 Validar o change com `npx @fission-ai/openspec validate documentacao-api --strict` e confirmar que passa.
