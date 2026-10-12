# Tasks

## 1. Implementação

- [x] 1.1 Duas configurações de CORS em `SecurityConfig`. Verificar por `AuthControllerIntegrationTest` (rotas de negócio de qualquer origem sem credenciais; pré-voo em `/api/auth/refresh` e `/api/auth/login` de origem não listada e listada) e pelos testes de CORS existentes.
- [x] 1.2 README e `.env.example`. Verificar a leitura.
- [x] 1.3 `./mvnw test` sem falhas e `openspec validate cors-qualquer-origem --strict` válido.
