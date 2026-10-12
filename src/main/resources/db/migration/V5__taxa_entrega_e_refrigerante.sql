-- Taxa de entrega do restaurante e refrigerante da refeicao (change restaurante-entrega-refrigerante-periodos).
-- Taxa: valor por entrega; cada refeicao servida e uma entrega por dia. Refrigerante: preco unitario, 1 por entrega.
-- NULL significa sem taxa / sem refrigerante: os registros existentes mantem o custo atual.
-- Parcelas do custo por dia e projecao por periodo sao calculadas pelo dominio; nada e gravado.
-- Valido em PostgreSQL e em H2 com MODE=PostgreSQL.

alter table restaurant add column delivery_fee numeric(19, 2);
alter table meal add column soda_price numeric(19, 2);
