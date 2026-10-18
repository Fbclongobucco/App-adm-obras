## 1. Domínio

- [x] 1.1 `Restaurant` com as duas taxas e `Meal.getDeliveryDailyPrice()`; testes R$ 452,00, R$ 454,00 e Jantar sem taxa do jantar
- [x] 1.2 `MealAdjustment` (`CHANGED`, entregas, refrigerantes, padrões, preço) e `Meal.putAdjustment` com a validação; testes R$ 489,00, R$ 484,00, R$ 432,00, R$ 163,00, R$ 266,00 e os códigos de erro
- [x] 1.3 `DayClosing`/`WeekClosing` com `CHANGED`, entregas separadas, `note` e números por refeição; total pela soma diária; testes R$ 2.305,00, R$ 4.575,00 e regressão R$ 4.231,00

## 2. Aplicação e infra

- [x] 2.1 V10, entidades e mappers de entidade; teste de migração copiando a taxa
- [x] 2.2 DTOs, mappers, use cases e os dois `PUT`; testes de integração (cria, substitui, restaurante atômico, jantar zerado, contrato do restaurante)
- [x] 2.3 OpenAPI e README; `./mvnw test` verde
