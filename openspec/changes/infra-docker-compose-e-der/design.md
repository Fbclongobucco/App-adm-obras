## Context

Ver `proposal.md` para a motivação. O que restringe as decisões:

**O `compose.yaml` atual define só o Postgres, e ele vai embora.** Suas três primeiras linhas dizem que é para "desenvolvimento com o perfil prod", apontando para as credenciais padrão de `application-prod.properties`. Com o banco externo, o arquivo perde o único serviço que tinha.

**Existe uma dependência de composição circular.** O `pom.xml` inclui `spring-boot-docker-compose`, que inspeciona o Compose da raiz e sobe os serviços عند do boot da aplicação. Hoje isso é inofensivo porque o único serviço é um Postgres que a aplicação consome de verdade. Vira um laço no momento em que o Compose passar a definir a própria aplicação: subir com o perfil `prod` tentaria iniciar um contêiner da aplicação de dentro da aplicação. Verifiquei que `spring.docker.compose.enabled=false` está **apenas** em `application-local.properties` e `application-test.properties` — nem no arquivo base nem em `application-prod.properties`.

**A aplicação já lê nove variáveis de ambiente**, e três delas (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) são as de conexão com o banco. Todas têm valor padrão no perfil `prod` — inclusive senha vazia. Ou seja: hoje nada falha por configuração ausente; a aplicação sobe e só quebra quando a primeira requisição toca o dado.

**`.gitignore` não trata `.env` nem `.env.example`.** Verifiquei: nenhum dos dois existe, e `.env` não é ignorado. Criar um `.env` real hoje resultaria em segredo commitado por acidente — o risco é real, não teórico.

**`HELP.md` não serve como documentação.** É boilerplate do Spring Initializr, está na primeira linha do `.gitignore` e não é versionado. Qualquer edição nele nunca chega ao repositório. Não existe README.

**Nenhuma ferramenta de PDF está instalada.** Sem graphviz `dot`, pandoc, LaTeX, PlantUML, mermaid-cli. Há `npx`/`node` e `python3` sem reportlab. Isso elimina de saída qualquer abordagem que dependa de binário de sistema.

**O schema são onze tabelas.** Verifiquei entidade por entidade, incluindo as duas tabelas de junção e uma coleção de elemento. O detalhamento está em D6 e é a verdade-terreno do diagrama.

## Goals / Non-Goals

**Goals:**
- Um `Dockerfile` que produz imagem de runtime enxuta, sem toolchain de build e sem root.
- Um Compose com exatamente um serviço, o da aplicação, e nenhum serviço de banco.
- `.env.example` fiel às nove variáveis reais, com obrigatoriedade declarada, e `.env` fora do versionamento.
- Configuração obrigatória ausente falha no boot nomeando a variável.
- Diagrama do schema em PDF na raiz, gerado de fonte textual versionada, sem depender de banco no ar.

**Non-Goals (limites de design, além do escopo do proposal):**
- Não hospedar,版本的 ou versionar a imagem em registry.
- Não introduzir serviço auxiliar, proxy, cache ou orquestração multi-stage de deploy.
- Não reverter a decisão de banco externo criando profile opcional com Postgres.
- Não gerar o diagrama a partir do banco ao vivo; isso amarraria a documentação a um ambiente.
- Não adicionar TLS, certificados ou gestão de segredos — o Compose entrega a aplicação localmente.

## Decisions

### D1. Dockerfile multi-stage: Maven constrói, JRE executa

**Escolha.** Estágio de build sobre imagem Maven, executando `./mvnw -DskipTests package`; estágio de execução sobre imagem JRE 25, copiando apenas o jar e executando com usuário não-root. Porta não-privilegiada exposta.

Alternativas descartadas:

- **Imagem único com Maven e JRE.** Rejeitada porque deixa toolchain de build e cache de dependências na imagem final — aumenta superfície e tamanho sem nenhuma função em execução.
- **Contêiner de build que sobe Maven em bind mount.** Rejeitada porque o artefato final passa a depender do estado do volume do host e o build deixa de ser repetível.
- **Usar `spring-boot:build-image` do Maven plugin.** É a via oficial do Spring Boot e produziria imagem OCI diretamente. Considerada seriously e preterida: o plugin está em versão mais nova que o resto do ecosystem aqui, e o multi-stage explícito é auditável sem depender de comportamento do plugin. Reavaliar se a build OCI passar a ser requisito.

**Ponto de atenção verificado.** O diretório `.mvn/wrapper/` contém apenas `maven-wrapper.properties` — o `maven-wrapper.jar` não está no repositório porque está no `.gitignore`. O estágio de build precisa de rede para obter o wrapper, ou de o jar ser montado. Isso é normal, mas significa que **o build do contêiner não é offline** e o `.dockerignore` não pode excluir `.mvn/`.

### D2. O laço de auto-orquestração é fechado desativando a integração, não removendo a dependência

**Escolha.** `spring.docker.compose.enabled=false` passa a valer também no perfil `prod` e no arquivo base, onde hoje não existe. Nenhum serviço de banco é adicionado ao Compose.

Alternativas descartadas:

- **Remover a dependência `spring-boot-docker-compose` do `pom.xml`.** Seria a solução mais limpa em tese, mas muda o escopo: a dependência é o que hoje permite `docker compose up -d` levantar o Postgres de desenvolvimento convenientemente. Removê-la é uma decisão sobre a estratégia de desenvolvimento que o usuário não pediu. Registrado como alternativa preferível se a dependência deixar de ter propósito.
- **Manter o serviço do Postgres no Compose para desenvolvimento.** Contradiz o requisito explícito e reintroduz a divergência entre dev e produção que motivou a mudança.

**Por que a falha é silenciosa se ignorada.** Não há erro visível: o Compose sobe, o contêiner da aplicação tenta subir outro contêiner da aplicação, e o sintoma é um contêiner que sai com erro de porta ocupada ou um processo que nunca sinaliza prontidão. É exatamente o tipo de problema que custa uma tarde. Por isso a desativação é tarefa com verificação própria, não um detalhe de config.

### D3. Segredos fora do versionamento por `.gitignore`, com negação explícita de `.env.example`

**Escolha.** `.gitignore` passa a ignorar `.env` e variantes (`*.env`, `.env.*`), com uma negação `!.env.example` para que o exemplo continue versionável. O Compose lê configuração por `env_file` apontando para `.env`.

A negação explícita é necessária porque `.env.*` casaria com `.env.example` e o padrão do Compose — arquivo local — não é o que se quer versionar.

**Por que `.env.example` e não um `env.exemplo` (sem ponto).** Sem o prefixo de ponto, o arquivo é Candidates natural a `source`-ar e o Compose não o trata como sobrescrita local por acidente; e a nomeação `.env.example` é a convenção que ferramentas de secret scanning já reconhecem. O pedido original dizia "env.exemple"; registro aqui a normalização para `.env.example`, que é a forma que interage corretamente com `env_file`.

### D4. Falha rápida por remoção dos defaults obrigatórios no perfil `prod`

**Escolha.** As três variáveis de conexão com banco perdem o valor padrão no perfil `prod`: `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` sem sufixo. O binding do Spring falha durante a inicialização com mensagem que nomeia o placeholder não resolvido. As demais, que são genuinamente opcionais, mantêm default.

Alternativas descartadas:

- **Bean `@ConfigurationProperties` com `@Validated` e campos `not blank`.** Dá mensagem melhor — "DB_URL é obrigatório" em vez de "Could not resolve placeholder". Custo: uma classe nova, e uma segunda fonte de verdade sobre o que é obrigatório, que precisa ser mantida em sincronia com o `.env.example`.
- **Validação em `@PostConstruct`.** Pior ainda: roda depois do binding, então o binding já teria falhado antes, ou a validação chega tarde demais.

**Trade-off aceito.** A mensagem padrão do Spring nomeia a variável, o que satisfaz o requisito de forma observável, mas não é uma mensagem scrit specially para o operador. Em troca, zero código novo e nenhuma lista de obrigatoriedade duplicada. Se a mensagem padrão for considerada insuficiente na prática, D4 é o ponto a revisar — e o `.env.example` passa a ser a única lista de obrigatoriedade a manter.

**Divergência deliberada em relação ao explore.** Esta change não muda a política de defaults; apenas move a responsabilidade de "preencher o `.env`" para "a aplicação recusar subir sem ele". Em desenvolvimento, `application-local.properties` continua apontando para H2 em memória e não é afetado.

### D5. PlantUML como dependência Maven em profile dedicada, fora do build padrão

**Escolha.** Dependência `plantuml` com escopo `provided`, mais `exec-maven-plugin` ligado a um profile `diagrams` que invoca a geração a partir do arquivo `.puml` e escreve o PDF na raiz. A ativação é explícita e por comando.

Alternativas descartadas:

- **Gerar o PDF dentro de um teste JUnit.** Era a opção mais atraente: o build passa a falhar quando o diagrama está desatualizado, sem passo manual. Descartada por um motivo concreto — PDF carrega data de geração nos metadados, então o byte sempre muda e a comparação falharia sempre. Não dá para usar "o PDF mudou" como detector de desatualização.
- **Graphviz via WASM com `npx`.** Layout de ER melhor, mas introduz toolchain Node num projeto Java sem Node no `pom.xml`.
- **Instalar graphviz no sistema.** Exige que toda máquina e o CI tenham o pacote. Contraria o requisito de reprodutibilidade sem dependência de sistema.

**Consequência honesta de D5.** Como o PDF não é determinístico byte a byte, "diagrama desatualizado" não é detectável por diff. O que substitui isso é o requisito de processo: a alteração de schema **inclui** a regeneração, revisada junto do `.puml` — que é texto e faz diff de verdade. O `.puml` é o artefato revisável; o PDF é o entregável. Isso está escrito no spec como "Diagram source is the reviewed artifact" em vez de uma promessa de detecção automática que a ferramenta não entrega.

### D6. O diagrama é escrito à mão em `.puml` a partir das entidades, com revisão por comparação

**Escolha.** Arquivo `.puml` na raiz, ao lado do PDF, escrito à mão a partir das entidades JPA e revisado por comparação com o schema que `ddl-auto` produz. PlantUML tem tipo de diagrama de entidade com cardinalidades.

**Por que não gerar o `.puml` por script a partir do código.** Um gerador seria preciso por construção, mas o projeto não tem JPAMetaModel nem biblioteca de introspecção de anotações em `pom.xml`, e a relação Employee-Project é assimétrica: `EmployeeEntity` **não** tem coleção `projects` — o muitos-para-muitos é propriedade só de `ProjectEntity`, e o lado do funcionário é montado no adaptador por consulta separada. Um gerador precisa hardcodar essa assimetria, o que transfere o problema em vez de resolvê-lo. Para onze tabelas estáveis, texto revisável é mais barato e mais honesto.

**Verdade-terreno: o schema que o diagrama precisa refletir.**

| Tabela | Colunas | Restrições |
|---|---|---|
| `address` | id, street, number, city, state(2), country, neighborhood, zip_code(8) | PK id; todas NOT NULL |
| `cost_center` | id, name, cnpj(14) | PK id; ambas NOT NULL |
| `client` | id, name, email, phone, address_id | PK id; email UNIQUE; address_id NOT NULL FK |
| `employee` | id, name, email, cpf(11), phone, address_id, birth_date, cost_center_id, role | PK id; email e cpf UNIQUE; cost_center_id NOT NULL FK; address_id nulável |
| `project` | id, os, description(500), cost_center_id, start_date, end_date, client_id, total_price(19,2), is_completed | PK id; os UNIQUE; start_date e is_completed NOT NULL; end_date nulável; FKs de cost_center e client nuláveis |
| `accommodation` | id, host_name, host_phone(11), address_id, capacity, days, is_contract, project_id, total_price(19,2) | PK id; tudo NOT NULL exceto project_id |
| `meal` | id, restaurant_name, price(19,2), is_billed, address_id, project_id, meal_type, quantity, date, total_price(19,2) | PK id; tudo NOT NULL exceto address_id e project_id; meal_type é enum |
| `project_employee` | project_id, employee_id | Tabela de junção, duas FKs |
| `accommodation_employee` | accommodation_id, employee_id | Tabela de junção, duas FKs |
| `app_user` | id, name, email, is_active, password_hash(100) | PK id; email UNIQUE |
| `user_role` | user_id, role | Coleção de elemento; role é enum |

Cardinalidades a representar: `address` 1:N `client`, 1:N `employee`, 1:N `accommodation`, 1:N `meal`. `cost_center` 1:N `employee` e 1:N `project`. `client` 1:N `project`. `project` 1:N `accommodation` e 1:N `meal`, nos dois casos com a FK no lado filho. `project` N:N `employee` e `accommodation` N:N `employee`, via as tabelas de junção. `app_user` 1:N `user_role`.

**O que o diagrama deliberadamente não mostra.** A restrição de unicidade de `(project_id, meal_type)` e a migração `V1` não entram, porque pertencem a `endurecer-dominio-e-persistencia`, que não foi implementada. Incluí-las agora descreveria um schema inexistente. A regeneração é tarefa desta change, a ser executada quando aquela entrar.

### D7. `README.md` novo como casa da documentação operacional

**Escolha.** README na raiz com: pré-requisitos, como copiar `.env.example` para `.env`, como subir com Compose, quais variáveis são obrigatórias, e como regenerar o diagrama. `HELP.md` fica intocado.

**Por que `HELP.md` não pode receber a documentação.** Ele está na primeira linha do `.gitignore` e não é versionado. Editá-lo produz uma alteração local que jamais chega ao repositório — o pior dos dois mundos: trabalho feito e documentação inexistente para quem clona. Criar README é o que efetivamente entrega a informação.

## Risks / Trade-offs

**[A dependência `spring-boot-docker-compose` continua no `pom.xml` e segue sendo um risco latente]** → Desativada explicitamente em todos os perfis. Removê-la de vez continua sendo a solução melhor e fica registrada como opção em D2 para quando alguém avaliar a estratégia de desenvolvimento.

**[Build da imagem exige rede para obter o Maven wrapper, que não está no repositório]** → Verificado e esperado. Mitigação: o `.dockerignore` preserva `.mvn/`, e o README registra que o primeiro build baixa dependências. Ambientes sem rede precisam de um espelho de registry ou de uma etapa de pré-cache — não resolvido aqui.

**[`MODE=PostgreSQL` e o banco externo trocam o H2 em memória por um Postgres real em desenvolvimento, o que muda o comportamento de dialecto nos testes manuais]** → profiles `local` e `test` continuam em H2; só o Compose usa o perfil de produção. Nenhum teste automatizado é afetado, porque `application-test.properties` não muda.

**[Diagrama desatualizado não é detectável automaticamente]** → Aceito em D5, com o processo de revisão do `.puml` como compensação. Um teste que comparasse o `.puml` gerado contra uma versão esperada seria a evolução natural, mas exige resolver a fonte gerada, que D5 optou por não ter.

**[Remover os defaults de `DB_PASSWORD` expõe uma senha vazia como erro em vez de falha silenciosa]** → É o objetivo. Quem dependia de default sem saber estarReading valor errado passa a ver erro de boot nomeando a variável.

**[O Compose deixa de prover o banco, então `docker compose up` deixa de ser suficiente para um desenvolvedor novo]** → É a decisão pedida. O README documenta que o banco precisa existir antes, e o `.env.example` traz a URL como placeholder a preencher. Mitigação real: instrução no README de como criar o banco e o usuário, sem prover o serviço.

**[`.env.example` pode divergir das properties se alguém renomear uma variável]** → Mitigação: a verificação da tarefa对应 compara o arquivo de exemplo contra os placeholders efetivamente resolvidos pelo projeto, não contra uma lista escrita à mão. Divergência passa a falhar em verificação, não em produção.

## Migration Plan

Ordem que evita deixar o repositório em estado quebrado entre commits.

1. **`.gitignore` e `.env.example`.** Primeiro porque é aditivo e não quebra nada. Verificar que `.env` passa a ser ignorado e `.env.example` continua versionável.
2. **README.** Antes do Compose, para que a documentação já exista quando o comando mudar de comportamento.
3. **`Dockerfile` e `.dockerignore`.** Verificar build da imagem e que o estágio final não tem toolchain nem root.
4. **Substituir o `compose.yaml`.** Ponto de maior risco por ser o único arquivo com dependência externa hoje. Verificar sobe com banco externo alcançável, e que não há serviço de banco nem volume de banco definido.
5. **Desativar a auto-orquestração.** Logo após o passo 4, porque é o que impede o laço. Verificar que subir com `prod` não invoca o Compose.
6. **Gerar o diagrama.** `docs`-free, na raiz: `.puml` e PDF. Verificar o PDF abrindo e conferindo contra a tabela de D6.
7. **Tarefas de coerência entre changes.** Ajustar a task 1.6 de `endurecer-dominio-e-persistencia` e criar nela a tarefa de regenerar o diagrama quando a `V1` entrar.

**Rollback.** Os passos 1 a 3 e 6 são puramente aditivos e reversíveis por remoção. O passo 4 é o único que remove funcionalidade existente (o Postgres de desenvolvimento): reverter é restaurar o `compose.yaml` anterior. O passo 5 é reversível removing a linha de configuração.

**Verificação de que o Compose está correto.** `docker compose config` renderiza a definição resolvida e evidencia serviço de banco remanescente ou variável não mapeada.

## Open Questions

Nenhuma que afete specs, abordagem ou quebra de tarefas.

Três pontos são deliberadamente adiados por serem ajustáveis sem reescrever nada:

- **Versão de imagem base do estágio de execução.** O tag exato de JRE 25 e de Maven 17 fica para o momento da implementação; a escolha não muda a estrutura.
- **Comando e nome exatos dos arquivos do diagrama.** O prefixo do nome do PDF e do `.puml` é escolha de implementação, desde que sejam adjacentes e previsíveis.
- **Se o `.env.example` deve incluir `SPRING_PROFILES_ACTIVE`.** A variável é lida pela aplicação e o spec exige cobertura completa, mas seu valor é um presets de uso e não um segredo; incluí-la com valor de exemplo resolve a questão sem decisão adicional.