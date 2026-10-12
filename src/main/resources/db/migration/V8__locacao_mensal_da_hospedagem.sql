-- Locacao mensal da hospedagem (change hospedagem-locacao-mensal).
-- A hospedagem passa a ter data de entrada e valor por mes; os periodos (primeiro e renovacoes) ficam em
-- accommodation_period. days e total_price saem: o total e a soma dos periodos, calculada no dominio.
-- Conversao dos dados atuais (dados de teste, confirmado pelo usuario): entrada = inicio da obra (sem obra,
-- a data de cadastro); valor mensal = total antigo (minimo 0.01, porque o dominio exige valor > 0); um
-- periodo da entrada ate a vespera do mesmo dia no mes seguinte (ultimo dia do mes quando o dia nao existe).
-- O id do primeiro periodo reaproveita o da hospedagem: ha um por hospedagem e evita funcao de UUID
-- especifica de banco. Valido em PostgreSQL e em H2 com MODE=PostgreSQL.

create table accommodation_period (
    id               uuid                        not null,
    accommodation_id uuid                        not null,
    start_date       date                        not null,
    end_date         date                        not null,
    price            numeric(19, 2)              not null,
    created_at       timestamp(6) with time zone not null,
    updated_at       timestamp(6) with time zone not null,
    constraint pk_accommodation_period primary key (id),
    constraint fk_accommodation_period_accommodation foreign key (accommodation_id)
        references accommodation (id) on delete cascade,
    constraint uk_accommodation_period_start unique (accommodation_id, start_date)
);

alter table accommodation add column start_date date;
alter table accommodation add column monthly_price numeric(19, 2);

update accommodation
set start_date    = coalesce((select p.start_date from project p where p.id = accommodation.project_id),
                             cast(accommodation.created_at as date)),
    monthly_price = case when total_price > 0 then total_price else 0.01 end;

insert into accommodation_period (id, accommodation_id, start_date, end_date, price, created_at, updated_at)
select id,
       id,
       start_date,
       case
           when extract(day from cast(start_date + interval '1' month as date)) = extract(day from start_date)
               then cast(start_date + interval '1' month as date) - 1
           else cast(start_date + interval '1' month as date)
       end,
       monthly_price,
       created_at,
       updated_at
from accommodation;

alter table accommodation alter column start_date set not null;
alter table accommodation alter column monthly_price set not null;
alter table accommodation drop column days;
alter table accommodation drop column total_price;
