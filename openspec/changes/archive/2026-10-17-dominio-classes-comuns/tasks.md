## 1. Linha de base

- [x] 1.1 Rodar `./mvnw test` antes de qualquer edição e registrar o resultado (número de testes e falhas pré-existentes), para comparar ao final — linha de base: 584 testes, 0 falhas, 0 erros

## 2. Enum solto para enum aninhado

- [x] 2.1 Mover `BillingPeriod` para dentro de `Meal` como `Meal.BillingPeriod` (mesmas constantes, mesma ordem, mesmo `getWorkingDays()`) e remover `core/domain/BillingPeriod.java`; verificar com `grep -rn 'core.domain.BillingPeriod'` sem ocorrências
- [x] 2.2 Ajustar referências em `MealProjection`, `application/dto/meal/MealProjectionDTO`, `MealTest` e `MealMapperTest` para `Meal.BillingPeriod`; verificar com `./mvnw -q compile test-compile`

## 3. Records para classes comuns

- [x] 3.1 Converter `AuditStamp` em classe `final` (construtor com a validação e o truncamento atuais, `createdAt()`, `updatedAt()`, `createdAt(Instant)`, `touchedAt(Instant)`, `equals`/`hashCode`/`toString`); verificar com `./mvnw test -Dtest=AuditStampTest`
- [x] 3.2 Converter `MealProjection` em classe `final` com os 7 acessores atuais e `equals`/`hashCode`/`toString`; verificar com `./mvnw test -Dtest='MealTest,MealMapperTest'`
- [x] 3.3 Converter `ClosingWeek` em classe `final` (acessores `start()`, `end()`, `partial()`, `current()`, métodos de instância e estáticos atuais, `equals`/`hashCode`/`toString`); verificar com `./mvnw test -Dtest=WeeklyClosingTest`
- [x] 3.4 Converter `DayClosing` em classe `final`, mantendo `DayClosing.Status` aninhado, `empty(...)`, `isServiceDay()` e `List.copyOf` em `adjustmentIds`; verificar com `./mvnw test -Dtest=WeeklyClosingTest`
- [x] 3.5 Converter `WeekClosing` e `WeekClosing.Totals` em classes `final` (Totals como `public static final class`), com `List.copyOf` em `days` no construtor; verificar com `./mvnw test -Dtest='WeeklyClosingTest,RestaurantUseCaseTest'`
- [x] 3.6 Confirmar que não restam `record` nem `enum` de topo em `core/domain`: `grep -rnE '^public (record|enum)|^\s+(public )?record ' src/main/java/com/longobuccodev/app_adm_obras/core/domain` sem ocorrências

## 4. Simplificação

- [x] 4.1 Em `WeekClosing`, trocar as agregações repetidas (totais da semana e soma por dia) por auxiliares privados únicos, sem alterar a regra de situação somada; verificar com `./mvnw test -Dtest=WeeklyClosingTest`

## 5. Verificação final

- [x] 5.1 Confirmar que fora de `core/domain` só mudaram imports de `BillingPeriod`: `git diff --stat` lista apenas `MealProjectionDTO`, `MealTest` e `MealMapperTest` fora do pacote
- [x] 5.2 Rodar `./mvnw test` completo e comparar com a linha de base do item 1.1: mesmo número de testes, nenhuma falha nova — resultado: 584 testes, 0 falhas, 0 erros
