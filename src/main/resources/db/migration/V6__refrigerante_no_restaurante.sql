-- Refrigerante passa da refeicao para o restaurante (change refrigerante-no-restaurante).
-- Preco unitario, 1 por entrega em todas as refeicoes do restaurante; NULL = restaurante nao fornece.
-- Cada restaurante recebe o maior preco ja informado nas refeicoes dele (MAX ignora NULL).
-- Restaurante com refeicoes com e sem refrigerante passa a fornecer para todas (decisao do usuario).
-- Valido em PostgreSQL e em H2 com MODE=PostgreSQL.

alter table restaurant add column soda_price numeric(19, 2);

update restaurant r
set soda_price = (select max(m.soda_price) from meal m where m.restaurant_id = r.id);

alter table meal drop column soda_price;
