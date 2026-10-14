## Purpose

Define como o refresh token é trocado e como o reuso é tratado, para que refreshes simultâneos legítimos do mesmo cliente não encerrem a sessão e o reuso tardio continue sendo tratado como roubo.

## ADDED Requirements

### Requirement: Reuso dentro da janela de tolerância renova a sessão
Um refresh token já trocado, reapresentado até `app.security.jwt.refresh-reuse-grace` depois da troca, SHALL receber um novo access token e um novo refresh token da mesma sessão, desde que a sessão siga aberta. A sessão MUST NOT ser revogada nesse caso, e o token emitido na troca anterior MUST continuar válido.

#### Scenario: Dois refreshes simultâneos com o mesmo token
- **WHEN** o refresh token A é trocado por B, e A é reapresentado 2 s depois
- **THEN** a API responde 200 com um novo par (C), e B e C continuam aceitos no refresh seguinte

#### Scenario: Disputa no mesmo instante
- **WHEN** duas requisições apresentam A ao mesmo tempo e só uma consegue marcá-lo como trocado
- **THEN** as duas respondem 200, cada uma com um refresh token diferente, e a sessão segue ativa

### Requirement: Reuso fora da janela revoga a sessão
Um refresh token já trocado, reapresentado depois de terminada a janela de tolerância, SHALL ser recusado com 401, e todos os refresh tokens da mesma sessão MUST ser revogados.

#### Scenario: Reapresentação tardia
- **WHEN** A foi trocado por B há mais de 30 s (janela padrão) e A é reapresentado
- **THEN** a resposta é 401, e B passa a ser recusado com 401

#### Scenario: Outros logins não são afetados
- **WHEN** a sessão de um login é revogada por reuso tardio
- **THEN** os refresh tokens de outro login do mesmo usuário continuam válidos

### Requirement: A janela não reabre sessão encerrada
A janela de tolerância MUST NOT emitir tokens para sessão encerrada por logout, por reuso tardio, por expiração ou por usuário inativo ou removido. Nesses casos o refresh SHALL responder 401.

#### Scenario: Reuso após logout
- **WHEN** A é trocado por B, o usuário faz logout com B, e A é reapresentado dentro da janela
- **THEN** a resposta é 401 e nenhum token é emitido

#### Scenario: Reuso após a validade absoluta
- **WHEN** A é reapresentado dentro da janela, mas depois do prazo absoluto da sessão
- **THEN** a resposta é 401 e nenhum token é emitido

#### Scenario: Usuário desativado
- **WHEN** A é reapresentado dentro da janela e o usuário foi desativado
- **THEN** a resposta é 401 e a sessão é revogada

### Requirement: Tokens emitidos na janela mantêm o prazo da sessão
Os refresh tokens emitidos pela janela de tolerância SHALL herdar a expiração absoluta do login original, sem estendê-la.

#### Scenario: Prazo preservado
- **WHEN** a janela emite o token C a partir de A
- **THEN** C expira no mesmo instante que A e B

### Requirement: Janela configurável
A duração da janela SHALL ser configurável por `app.security.jwt.refresh-reuse-grace`, com padrão de 30 s. O valor `0` MUST desligar a tolerância, de modo que qualquer reuso revogue a sessão. Valor negativo, ou maior ou igual à validade do access token, MUST impedir a aplicação de iniciar.

#### Scenario: Tolerância desligada
- **WHEN** `refresh-reuse-grace=0` e A é reapresentado 1 s depois de trocado
- **THEN** a resposta é 401 e a sessão é revogada

#### Scenario: Valor inválido
- **WHEN** `refresh-reuse-grace=-5s`
- **THEN** a aplicação não inicia e informa a propriedade inválida
