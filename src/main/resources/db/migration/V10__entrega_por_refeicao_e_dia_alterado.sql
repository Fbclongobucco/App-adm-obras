-- Taxa de entrega por refeicao e numeros do dia nos ajustes (change refeicoes-editaveis-por-dia).
-- restaurant: delivery_fee vira lunch_delivery_fee e dinner_delivery_fee; a taxa atual vale para as duas,
-- sem mudar nenhum valor calculado.
-- meal_adjustment: novo tipo CHANGED (dia util com outros numeros), sem mudanca de coluna (type e varchar).
-- lunch_deliveries, dinner_deliveries e sodas: opcionais em EXTRA_DAY e CHANGED; null = padrao do dominio.
-- Valido em PostgreSQL e em H2 com MODE=PostgreSQL.

alter table restaurant add column lunch_delivery_fee numeric(19, 2);
alter table restaurant add column dinner_delivery_fee numeric(19, 2);
update restaurant set lunch_delivery_fee = delivery_fee, dinner_delivery_fee = delivery_fee;
alter table restaurant drop column delivery_fee;

alter table meal_adjustment add column lunch_deliveries integer;
alter table meal_adjustment add column dinner_deliveries integer;
alter table meal_adjustment add column sodas integer;
