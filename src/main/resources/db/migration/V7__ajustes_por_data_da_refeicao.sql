-- Ajustes por data da refeicao (change refeicao-ajustes-por-data).
-- EXTRA_DAY: sabado ou domingo com fornecimento, com as quantidades do dia; soma ao total.
-- NO_SERVICE: dia util sem fornecimento, sem quantidades; desconta um dia util.
-- Regras (periodo da obra, dia da semana, quantidades) e valores sao do dominio; nada e calculado aqui.
-- Uma data por refeicao; excluir a refeicao exclui os ajustes.
-- Valido em PostgreSQL e em H2 com MODE=PostgreSQL.

create table meal_adjustment (
    id              uuid                        not null,
    meal_id         uuid                        not null,
    date            date                        not null,
    type            varchar(20)                 not null,
    lunch_quantity  integer,
    dinner_quantity integer,
    note            varchar(100),
    created_at      timestamp(6) with time zone not null,
    updated_at      timestamp(6) with time zone not null,
    constraint pk_meal_adjustment primary key (id),
    constraint fk_meal_adjustment_meal foreign key (meal_id) references meal (id) on delete cascade,
    constraint uk_meal_adjustment_meal_date unique (meal_id, date)
);
