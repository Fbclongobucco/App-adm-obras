## Context

Motivação em `proposal.md`. Requisitos em `specs/api-documentation/spec.md`.

Estado atual relevante:

- **Spring Boot 4.1.1, Java 25, Spring MVC** (`spring-boot-starter-webmvc`). Não há WebFlux.
- **Segurança.** `SecurityConfig` tem um único `SecurityFilterChain`: `POST /api/auth/login` e `/error` liberados; `GET /api/**` para `ADMIN`/`OPERADOR`; demais `/api/**` só `ADMIN`; `anyRequest().authenticated()`. A autenticação é HTTP Basic, sem token. `/api/auth/login` só valida as credenciais e devolve o perfil; cada requisição seguinte envia Basic de novo.
- **Controllers** em `infra/controllers/`, 9 classes sob `/api`, 55 mapeamentos. Nenhuma anotação OpenAPI.
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) trata 4 tipos de exceção, sem handler genérico de `Exception`. Uma rota inexistente segue o tratamento padrão do Spring.
- **CORS** registrado só para `/api/**`. O Swagger UI é servido pela própria aplicação (mesma origem), sem impacto de CORS.
- **Fronteira de camadas.** `core/` e `application/` não importam framework. Nada deste change toca essas camadas.
- **Perfis.** `local` e `test` usam H2 em memória; `prod` usa PostgreSQL. O perfil padrão é `local`.

## Goals / Non-Goals

**Goals:**
- Descrição OpenAPI e Swagger UI gerados do código, sem anotar endpoint por endpoint.
- Ativação por perfil, com desligado como padrão.
- Autenticação Basic utilizável pela interface.

**Non-Goals:**
- Enriquecer a descrição com `@Operation`, `@ApiResponse` ou exemplos.
- Agrupar a API em múltiplos documentos (`GroupedOpenApi`).
- Proteger a documentação com autenticação. Onde está ativa, é pública; em `prod`, não existe.

## Decisions

### D1. springdoc-openapi, starter WebMVC UI, versão fixada

**Escolha.** `org.springdoc:springdoc-openapi-starter-webmvc-ui`. Ele traz a geração da descrição (`/v3/api-docs`) e o Swagger UI empacotado (`/swagger-ui/index.html`, com redirecionamento de `/swagger-ui.html`).

**Versão.** O parent do Spring Boot não gerencia essa dependência, então a versão fica explícita no `pom.xml`. Verificado no Maven Central em 2026-10-10: a release mais recente é `3.1.1`, e o POM `springdoc-openapi:3.1.1` usa como parent o Spring Boot `4.1.0`. A diferença para o `4.1.1` do projeto é de patch. A compatibilidade efetiva é confirmada na implementação, com a aplicação subindo e a suíte de testes verde (task 1.2). Se `3.1.1` falhar contra `4.1.1`, ajustar para a release compatível seguinte é mudança de uma linha.

**Alternativas descartadas.**
- *SpringFox.* Sem manutenção, incompatível com Spring Boot 3+.
- *Arquivo OpenAPI escrito à mão.* Desatualiza ao primeiro endpoint novo e contraria o requisito de geração a partir do código.

### D2. Desligado por padrão; ligado só em `local` e `test`

**Escolha.**
- `application.properties`: `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false`.
- `application-local.properties` e `application-test.properties`: os dois em `true`.
- `application-prod.properties`: os dois em `false`, explícitos. Redundante com o padrão, mas deixa a intenção visível onde importa e protege contra uma inversão futura do padrão.

Com `api-docs.enabled=false`, o springdoc não registra os endpoints. O caminho passa a ser uma rota inexistente. Como a liberação de D3 é incondicional, anônimo e autenticado recebem 404. Nenhuma descrição é entregue, que é o que a spec exige.

**Alternativas descartadas.**
- *`@Profile` numa classe de configuração.* Esconderia a regra em código. A propriedade é o mecanismo nativo do springdoc e fica no mesmo lugar que as outras diferenças por perfil.
- *Ligado por padrão e desligado só em `prod`.* Um perfil novo, por exemplo `staging`, nasceria expondo a API. O padrão desligado falha para o lado seguro.

### D3. Liberação dos caminhos no `SecurityFilterChain` existente, incondicional

**Escolha.** Adicionar ao chain atual, antes de `anyRequest()`:

```
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
```

A regra é incondicional, sem depender do perfil. Em `prod` os handlers não existem (D2), e um `permitAll` sobre rota inexistente não expõe nada: o anônimo passa pelo filtro e recebe 404.

**Alternativas descartadas.**
- *`WebSecurityCustomizer.ignoring()`.* Tira os caminhos de todo o filtro de segurança, inclusive cabeçalhos de proteção. O Spring Security desaconselha esse uso para recursos servidos pela aplicação.
- *Segundo `SecurityFilterChain` condicionado a perfil.* Mais código e mais ordem de chains para raciocinar, para um ganho nulo, já que em `prod` não há o que proteger.

### D4. Metadados e esquema HTTP Basic declarados num bean `OpenAPI`

**Escolha.** Nova classe `infra/config/OpenApiConfig.java` com um bean `OpenAPI`:
- `info`: título `app-adm-obras API`, versão vinda do artefato (`0.0.1-SNAPSHOT` hoje), sem texto inventado.
- `components.securitySchemes`: esquema `basicAuth` (`type: http`, `scheme: basic`).
- `security`: requisito global `basicAuth`, para o botão "Authorize" valer para todas as operações.

`POST /api/auth/login` é público, mas herda o requisito global na descrição. Isso só afeta a apresentação na UI (cadeado), não o acesso real. Tratar como exceção exigiria anotar o controller, o que fica fora do escopo (non-goal).

A classe fica em `infra/`, como o resto da configuração de framework. Nenhuma dependência de springdoc entra em `core/` ou `application/`.

### D5. Ordenação e agrupamento padrão

Agrupamento por tag padrão do springdoc, uma por controller. Sem ordenação customizada. Atende o cenário "rotas agrupadas por recurso" sem configuração.

## Risks / Trade-offs

**[springdoc 3.1.1 incompatível com Spring Boot 4.1.1]** → O POM declara Boot 4.1.0 como base. A task 1.2 sobe a aplicação e roda a suíte antes de qualquer outra coisa. Se falhar, fixar outra versão é ajuste de uma linha, sem efeito no restante do plano.

**[`@RestControllerAdvice` gera respostas de erro em todas as operações da descrição]** → O springdoc incorpora os handlers globais como respostas possíveis de cada operação. É ruído cosmético, não erro. Aceito na v1; refinar exigiria anotações (non-goal).

**[Perfil `test` usado fora de testes automatizados]** → O comentário de `application-test.properties` indica uso também para "testes manuais da API", com usuário bootstrap. Ligar a documentação ali é intencional. Quem subir um servidor compartilhado com `test` expõe a documentação; isso já acontece hoje com o admin padrão, então o perfil não deve ir para ambiente compartilhado.

**[Descrição imprecisa por inferência]** → Sem anotações, campos obrigatórios e formatos vêm só dos tipos Java dos DTOs. O formato de erro (`ErrorResponseDTO`) aparece via advice. Aceito na v1.

**[Testes `@WebMvcTest` existentes]** → `SecurityConfigTest` importa `SecurityConfig`. A nova regra `permitAll` não altera os casos atuais. A suíte completa é rodada na task 2.4.

## Migration Plan

1. Dependência e propriedades por perfil. Subir em `local` e confirmar o boot.
2. Regra de segurança e `OpenApiConfig`.
3. Testes de integração dos dois lados: ativo em `test`, ausente em `prod`.
4. README.

**Rollback.** Remover a dependência, as propriedades, a regra de segurança e a classe de configuração. Não há estado persistido nem migração de dados.

## Open Questions

Nenhuma que altere specs, abordagem ou tasks.
