## 1. Gitignore e variáveis de ambiente

- [ ] 1.1 Ajustar `.gitignore` para ignorar `.env` e variantes, mas manter `.env.example` versionável; verificar que `.env.example` aparece como tracked e `.env` é ignorado com `git check-ignore -v`
- [ ] 1.2 Criar `.env.example` com as nove variáveis efetivamente lidas (`SPRING_PROFILES_ACTIVE`, `APP_CORS_ALLOWED_ORIGINS`, `APP_BOOTSTRAP_ENABLED`, `APP_BOOTSTRAP_ADMIN_NAME`, `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), marcando obrigatoriedade e defaults, e verificar que toda variável real aparece e nenhuma está faltando
- [ ] 1.3 Verificar, por análise dos placeholders em `application*.properties`, que o `.env.example` cobre exatamente o consumido e que os defaults declarados batem com os efetivos

## 2. Documentação

- [ ] 2.1 Criar `README.md` na raiz com pré-requisitos, instruções para copiar `.env.example` para `.env`, como subir com Compose e banco externo, como gerar o diagrama e como interpretar o `.env`; verificar que `HELP.md` permanece ignorado pelo Git e não é modificado
- [ ] 2.2 Verificar que nenhuma alteração é feita em `HELP.md` (git status não deve apontar modificação nele) e que o README cobre as variáveis obrigatórias e o caminho para regerar o diagrama

## 3. Falha rápida por configuração

- [ ] 3.1 Em `application-prod.properties` (e onde aplicável) remover defaults que permitem subir sem banco obrigatório, de modo que `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` tenham de ser resolvidos, e verificar que a aplicação falha ao iniciar com variável ausente nomeando-a
- [ ] 3.2 Adicionar `spring.docker.compose.enabled=false` explicitamente em todos os perfis onde falte (incluindo base/prod), para evitar que a aplicação tente orquestrar-se ao ser executada a partir do Compose; verificar por grep que não existe perfil sem essa linha
- [ ] 3.3 Subir com configuração incompleta e verificar o log de inicialização, e subir com configuração completa e confirmar que a inicialização não falha por configuração

## 4. Dockerfile, .dockerignore e Compose

- [ ] 4.1 Criar `.dockerignore` adequado ao multi-stage, excluindo build/target desnecessários, arquivos IDE e artefatos temporários; verificar que `target/` pré-existente não está copiado indevidamente
- [ ] 4.2 Criar `Dockerfile` multi-stage com estágio de build (Maven) e estágio de runtime (JRE), processo não-root, porta não-privilegiada e exposição da porta; executar `docker build` sem testes e confirmar que a imagem final não contém Maven, fontes do projeto, nem `maven-repo` de build
- [ ] 4.3 Substituir `compose.yaml` para que defina apenas o serviço da aplicação (nome, build a partir do Dockerfile, portas, `env_file` apontando para `.env`, variáveis quando necessário), removendo completamente serviço `postgres` e volume `app-adm-obras-pgdata`; executar `docker compose config` e verificar que não há serviço postgres nem volume postgres
- [ ] 4.4 Executar `docker compose up` com banco externo alcançável e com `.env` válido, confirmar que o contêiner sobe, passa ao estado healthy/pronto e responde a requisições, e depois `docker compose down` sem deixar serviços de banco
- [ ] 4.5 Verificar que a imagem gerada não executa como root e escuta em porta não-privilegiada, inspecionando o contêiner em execução

## 5. Diagrama do schema (PDF) com fonte versionada

- [ ] 5.1 Adicionar dependência e plugin para geração do diagrama via Maven, de modo que a geração seja acionada apenas com o profile `diagrams` (não no build padrão), executando a ferramenta de geração e produzindo PDF na raiz; documentar o comando no README
- [ ] 5.2 Criar o arquivo de fonte textual do diagrama (formato compatível com a ferramenta escolhida) descrevendo todas as 11 tabelas: address, cost_center, client, employee, project, accommodation, meal, project_employee, accommodation_employee, app_user, user_role, com colunas, PKs, FKs, cardinalidades e restrições de unicidade
- [ ] 5.3 Gerar o PDF do DER a partir da fonte e commitar tanto o arquivo-fonte quanto o PDF na raiz; verificar que o PDF é arquivo válido e que ambos aparecem no `git ls-files`
- [ ] 5.4 Confrontar o diagrama com as entidades JPA: toda tabela aparece, as junções aparecem, coleções de elemento aparecem, e o schema refletido é o **atual** (não incluindo a restrição de unicidade de `(project_id, meal_type)` da outra change); registrar a proveniência no diagrama
- [ ] 5.5 Verificar que a geração não exige banco em execução e que é reproduzível, executando o comando duas vezes e comparando que não há alteração inesperada (ou que a fonte determina o artefato)

## 6. Coerência com a change anterior `endurecer-dominio-e-persistencia`

- [ ] 6.1 Reescrever a task 1.6 daquela change para não apontar para o `compose.yaml` na raiz como local para documentar a recriação do `pgdata`, fornecendo o texto correto conforme o novo `compose.yaml` (que não define Postgres), e justificar a mudança
- [ ] 6.2 Adicionar àquela change uma nova task (ou ajustar 2.1/7) que registre que, quando a `V1` for aplicada, o diagrama DER deve ser regenerado para refletir a restrição de unicidade de `meal(project_id, meal_type)`; verificar o arquivo final da change anterior não contém referência ao serviço postgres que foi removido
- [ ] 6.3 Verificar que o ajuste à change anterior não altera código do projeto, apenas seu arquivo de planning (`tasks.md` e/ou `design.md` se necessário) e não quebra a validação daquela change
- [ ] 6.4 Checar que as duas changes são mutuamente coerentes: os defaults removidos em prod não conflitam com o que o `.env.example` da nova change documenta

## 7. Verificação final

- [ ] 7.1 Executar `docker compose config` e garantir que o Compose renderizado não lista `postgres` nem volume relacionado
- [ ] 7.2 Executar o build completo sem testes e depois a suíte de testes, verificando que tudo permanece verde
- [ ] 7.3 Validar a nova change com `openspec validate --strict` e, caso a anterior tenha sido ajustada, também validar aquela, garantindo que ambas passam
- [ ] 7.4 Realizar verificação manual: novo desenvolvedor consegue clonar, copiar `.env.example` para `.env` com banco externo real, `docker compose up` e obter a aplicação respondendo, sem subir Postgres pelo Compose, e o PDF existe e corresponde às entidades