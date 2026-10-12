-- Refeicao como configuracao da obra (change refeicoes-por-obra-e-email-opcional).
-- Sem data: quantidade e preco por dia de cada refeicao servida (almoco, jantar ou os dois).
-- Custo por dia, dias uteis e total sao calculados pelo dominio a partir da obra; nada e gravado.
-- Valido em PostgreSQL e em H2 com MODE=PostgreSQL.

alter table meal add column lunch_quantity integer;
alter table meal add column lunch_price numeric(19, 2);
alter table meal add column dinner_quantity integer;
alter table meal add column dinner_price numeric(19, 2);

-- Coberturas da obra: TRUE quando a refeicao serve almoco/jantar, NULL quando nao serve.
-- UNIQUE trata NULL como distinto no PostgreSQL e no H2, entao so coberturas ocupadas conflitam.
-- Indice parcial nao e usado porque o H2 nao suporta.
alter table meal add column lunch_slot boolean;
alter table meal add column dinner_slot boolean;

-- Ate o V3 havia no maximo um LUNCH e um DINNER por obra (uk_meal_project_type):
-- a conversao nao gera cobertura repetida.
update meal
set lunch_quantity = quantity,
    lunch_price    = price,
    lunch_slot     = true
where meal_type = 'LUNCH';

update meal
set dinner_quantity = quantity,
    dinner_price    = price,
    dinner_slot     = true
where meal_type = 'DINNER';

alter table meal drop constraint uk_meal_project_type;
alter table meal add constraint uk_meal_project_lunch unique (project_id, lunch_slot);
alter table meal add constraint uk_meal_project_dinner unique (project_id, dinner_slot);

alter table meal drop column date;
alter table meal drop column quantity;
alter table meal drop column price;
alter table meal drop column total_price;

-- E-mail opcional em cliente e colaborador. uk_client_email e uk_employee_email continuam
-- valendo para valores nao nulos; varios NULL nao conflitam.
alter table client alter column email drop not null;
alter table employee alter column email drop not null;
