## 1. Setup and configuration

- [ ] 1.1 Criar diretório `scripts/` e verificar que o diretório existe e está versionável; confirmar que não há código de aplicação alterado
- [ ] 1.2 Atualizar `.env.example` adicionando variáveis `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` (ou `DOCKERHUB_PASSWORD`), `DOCKERHUB_REPOSITORY`, `DOCKERHUB_TAGS` (opcional), `DOCKER_IMAGE_NAME` (opcional), `BUILD_CONTEXT` (opcional), `DOCKERFILE_PATH` (opcional), com placeholders e descrição de obrigatoriedade; verificar que todas aparecem no exemplo e que nenhum valor sensível foi adicionado
- [ ] 1.3 Atualizar `README.md` com seção "Docker build & push" contendo variáveis, exemplo de uso, segurança (preferir token, `docker logout`) e que o script não registra credenciais; verificar que `HELP.md` não foi modificado

## 2. Build and push script

- [ ] 2.1 Criar `scripts/docker-build-push.sh` executável: validar variáveis obrigatórias (username, token/password, repository), validar existência de `Dockerfile`, derivar tags padrão (latest + short git SHA se disponível), construir com `docker build`, autenticar com `docker login --password-stdin`, aplicar tags, fazer push de cada tag; em caso de falha reportar fase (build/auth/push) e sair com código não-zero
- [ ] 2.2 Garantir que o script não imprime credenciais (usar `--password-stdin`, não `echo` de segredos), e só reporta referências, tags, digest e mensagens genéricas; validar por inspeção do script
- [ ] 2.3 Após push bem-sucedido, tentar obter digest via `docker inspect --format='{{index .RepoDigests 0}}'` ou fallback para não falhar se indisponível; imprimir referências completas e digest quando disponível; verificar o formato da saída
- [ ] 2.4 Testar localmente com `.env` válido (se disponível) ou com variáveis temporárias: build+push para repositório de teste; confirmar que referências reportadas permitem `docker pull`; em seguida remover imagem/teste se desejado
- [ ] 2.5 Verificar comportamento com variáveis ausentes: credenciais faltando → falha antes de push com mensagem clara; Dockerfile ausente → falha na fase de build com mensagem clara

## 3. Integration and verification

- [ ] 3.1 Executar `docker compose config` para garantir que mudanças não afetam o Compose existente da outra change (nenhuma alteração ao compose nesta change)
- [ ] 3.2 Executar suíte de testes completa e confirmar que nada foi quebrado (nenhuma alteração em código da aplicação)
- [ ] 3.3 Validar a change com `openspec validate --strict` e confirmar que passa
- [ ] 3.4 Revisar todos os arquivos criados/modificados: apenas `scripts/docker-build-push.sh`, `.env.example`, `README.md`; nenhum código-fonte Java, nenhum arquivo de runtime da aplicação modificado