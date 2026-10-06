## Why

O projeto não tem como ser executado hoje de forma reprodutível fora da máquina de desenvolvimento. Não existe `Dockerfile`, e o `compose.yaml` da raiz define **apenas** um Postgres — que, por decisão de infraestrutura, agora será **externo**. Ou seja, o arquivo de orquestração existente serve ao componente que deixa de existir aqui, e não há artefato algum que suba a aplicação.

Some-se a isso a ausência de qualquer documentação do schema. Onze tabelas, duas tabelas de junção e uma coleção de elemento estão definidas só em anotações JPA. Nenhum diagrama, nenhuma referência — quem integra, audita ou migra o banco não tem onde olhar.

## What Changes

### Execução em contêiner

- **`BREAKING`** O `compose.yaml` da raiz deixa de definir o serviço `postgres` e passa a definir a **aplicação**. O serviço de banco e o volume `app-adm-obras-pgdata` desaparecem do Compose, porque o banco passa a ser externo.
- `Dockerfile` multi-stage: estágio de build com Maven producing o jar, estágio de execução com JRE e apenas o artefato. Sem toolchain de build na imagem final.
- A imagem de execução roda com usuário não-root e a aplicação escutando em porta não-privilegiada.
- **Correção de uma armadilha de laço.** O projeto tem a dependência `spring-boot-docker-compose`, que sobe serviços do Compose na inicialização. Hoje `spring.docker.compose.enabled=false` só existe nos perfis `local` e `test`; o perfil `prod` não tem essa linha. Com o Compose passando a definir a própria aplicação, subir com `prod` tentaria iniciar um contêiner da aplicação de dentro da aplicação. A desativação passa a ser explícita também onde o perfil automáticamente a assume.
- **Documentação operacional em `README.md` novo.** O repositório não tem README. `HELP.md` existe, mas é boilerplate do Initializr, está no `.gitignore` (primeira linha) e não é versionado — editar isso produziria alteração nunca commitada. O README passa a ser o lugar versionado de como rodar com Compose, quais variáveis configurar e como regenerar o diagrama. `HELP.md` é deixado em paz.

### Configuração por ambiente

- `.env.example` na raiz, documentando as **nove** variáveis que a aplicação efetivamente lê, cada uma com valor de exemplo e comentário do que é.
- `.env` real passa a ser ignorado pelo Git; `.env.example` passa a ser versionado. Hoje `.gitignore` não trata nenhum dos dois.
- Configuração ausente ou inválida falha na inicialização com mensagem que identifica a variável, em vez de a aplicação subir e falhar na primeira requisição.
- A conexão com o banco externo passa a ser obrigatória em produção: sem URL, usuário ou senha, o boot falha com mensagem explícita.

### Documentação do schema

- Diagrama entidade-relacionamento do banco, em PDF, na raiz do repositório.
- A fonte do diagrama é um arquivo de texto versionado e revisável, não um artefato binário editado à mão. O PDF é gerado a partir dele por comando documentado, para que possa ser regenerado.
- A geração usa PlantUML como dependência Maven: roda em Java puro, sem browser e sem binário de sistema. Nenhuma ferramenta de PDF está instalada nesta máquina hoje — sem graphviz, pandoc, latex, plantuml ou mermaid-cli — então a ausência de dependência de sistema é o que torna a geração reproduzível no CI e por qualquer desenvolvedor.
- O diagrama reflete o schema atual, derivado das entidades JPA. **Não** inclui a migração `V1` nem a restrição de unicidade de refeição planejadas na change `endurecer-dominio-e-persistencia`, porque aquela change ainda não foi implementada. A regeneração quando ela entrar é tarefa explícita.

### Coerência entre as duas changes

- A task 1.6 de `endurecer-dominio-e-persistencia` manda documentar a recriação do volume `app-adm-obras-pgdata` dentro do `compose.yaml`. Como este arquivo deixa de ter serviço Postgres, essa instrução fica impossível de cumprir como escrita. Ela é reescrita para que a documentação vá para um lugar que ainda exista.

### Não-objetivos

- **Não** é provisionado Postgres, nem local, nem remoto, nem via profile opcional do Compose. O banco é externo e sua operação não pertence a este repositório.
- **Não** há serviço auxiliar novo. A aplicação não usa Redis, fila, e-mail ou storage; introduzi-los seria speculative generality.
- **Não** há orquestração de deploy, registry, healthcheck orquestrado ou HTTPS. O Compose entrega a aplicação localmente; o que acontece com a imagem depois é outro problema.
- **Não** há mudança de contrato da API nem de código de aplicação.

## Capabilities

### New Capabilities

- `app-containerization`: a aplicação é executável como contêiner via Docker Compose, conectando-se a um banco de dados externo que o Compose não gerencia.
- `runtime-configuration`: a configuração da aplicação vem de variáveis de ambiente, é documentada por um exemplo versionado sem segredos, e falha de forma explícita quando está incompleta.
- `database-documentation`: o schema do banco possui um diagrama entidade-relacionamento versionado em PDF, gerado a partir de fonte textual revisável.

### Modified Capabilities

(nenhuma — o projeto ainda não possui specs)

## Impact

**Arquivos novos na raiz**
- `Dockerfile` e `.dockerignore`
- `README.md` — não existe hoje, e `HELP.md` não serve como substituto porque é ignorado pelo Git
- `.env.example`
- PDF do DER, com o arquivo-fonte do diagrama ao lado

**Arquivos substituídos**
- `compose.yaml` — deixa de ter `postgres` e passa a ter o serviço da aplicação

**Arquivos alterados**
- `.gitignore` — passa a ignorar `.env` e a versionar `.env.example`
- `pom.xml` — recebe a dependência de geração do diagrama em escopo de build

**Configuração**
- Perfil que hoje não desativa a integração de Docker Compose passa a desativar explicitamente, para evitar o laço de subir a aplicação de dentro dela.

**Interação com `endurecer-dominio-e-persistencia`**
- Reescrita da task 1.6 daquela change.
- Aquela change adiciona Flyway com `ddl-auto=none`. Esta change não mexe em Flyway nem nas properties de datasource, mas as duas tocam arquivos de properties diferentes e o resultado combinado precisa continuar coerente: o `.env.example` documenta `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, que são exatamente as variáveis que o perfil `prod` já lê.

**Testes**
- Nenhum teste de aplicação existente é afetado. A verificação é por execução real do contêiner e por inspeção do diagrama gerado.