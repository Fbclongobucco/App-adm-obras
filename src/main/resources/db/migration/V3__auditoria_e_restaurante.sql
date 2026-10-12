-- Auditoria: instante de criacao e de ultima alteracao, definidos pelo dominio.
-- Linhas existentes recebem o instante da migracao.

alter table cost_center add column created_at timestamp(6) with time zone;
alter table cost_center add column updated_at timestamp(6) with time zone;
update cost_center set created_at = current_timestamp, updated_at = current_timestamp;
alter table cost_center alter column created_at set not null;
alter table cost_center alter column updated_at set not null;

alter table client add column created_at timestamp(6) with time zone;
alter table client add column updated_at timestamp(6) with time zone;
update client set created_at = current_timestamp, updated_at = current_timestamp;
alter table client alter column created_at set not null;
alter table client alter column updated_at set not null;

alter table employee add column created_at timestamp(6) with time zone;
alter table employee add column updated_at timestamp(6) with time zone;
update employee set created_at = current_timestamp, updated_at = current_timestamp;
alter table employee alter column created_at set not null;
alter table employee alter column updated_at set not null;

alter table project add column created_at timestamp(6) with time zone;
alter table project add column updated_at timestamp(6) with time zone;
update project set created_at = current_timestamp, updated_at = current_timestamp;
alter table project alter column created_at set not null;
alter table project alter column updated_at set not null;

alter table accommodation add column created_at timestamp(6) with time zone;
alter table accommodation add column updated_at timestamp(6) with time zone;
update accommodation set created_at = current_timestamp, updated_at = current_timestamp;
alter table accommodation alter column created_at set not null;
alter table accommodation alter column updated_at set not null;

alter table meal add column created_at timestamp(6) with time zone;
alter table meal add column updated_at timestamp(6) with time zone;
update meal set created_at = current_timestamp, updated_at = current_timestamp;
alter table meal alter column created_at set not null;
alter table meal alter column updated_at set not null;

alter table app_user add column created_at timestamp(6) with time zone;
alter table app_user add column updated_at timestamp(6) with time zone;
update app_user set created_at = current_timestamp, updated_at = current_timestamp;
alter table app_user alter column created_at set not null;
alter table app_user alter column updated_at set not null;

-- Ordenacao do dashboard: obras alteradas mais recentemente
create index idx_project_updated_at on project (updated_at);

-- Restaurante: atende exatamente uma obra. A refeicao passa a referenciar o restaurante;
-- meal.project_id continua como copia da obra do restaurante, para uk_meal_project_type.

create table restaurant (
    id          uuid                        not null,
    name        varchar(100)                not null,
    phone       varchar(11),
    address_id  uuid,
    project_id  uuid                        not null,
    created_at  timestamp(6) with time zone not null,
    updated_at  timestamp(6) with time zone not null,
    constraint pk_restaurant primary key (id),
    constraint fk_restaurant_address foreign key (address_id) references address (id),
    constraint fk_restaurant_project foreign key (project_id) references project (id)
);

create index idx_restaurant_project on restaurant (project_id);
create index idx_restaurant_address on restaurant (address_id);

-- Um restaurante por par (obra, nome) das refeicoes existentes. Reaproveita o id e o endereco
-- da refeicao de menor id do par: ids nao colidem entre tabelas, e o endereco deixa de ser da
-- refeicao (coluna removida abaixo), entao nao fica compartilhado. Gerar UUID em SQL nao e
-- portavel entre H2 e PostgreSQL.
insert into restaurant (id, name, address_id, project_id, created_at, updated_at)
select source.id, substring(source.restaurant_name from 1 for 100), source.address_id, source.project_id,
       current_timestamp, current_timestamp
from meal source
where source.project_id is not null
  and cast(source.id as varchar(36)) = (
      select min(cast(other.id as varchar(36)))
      from meal other
      where other.project_id = source.project_id
        and other.restaurant_name = source.restaurant_name);

alter table meal add column restaurant_id uuid;

update meal
set restaurant_id = (
    select restaurant.id
    from restaurant
    where restaurant.project_id = meal.project_id
      and restaurant.name = substring(meal.restaurant_name from 1 for 100));

-- Falha de proposito se existir refeicao sem obra: nao ha obra para criar o restaurante.
-- Antes de migrar um banco com dados: select count(*) from meal where project_id is null
alter table meal alter column restaurant_id set not null;
alter table meal alter column project_id set not null;
alter table meal add constraint fk_meal_restaurant foreign key (restaurant_id) references restaurant (id);
create index idx_meal_restaurant on meal (restaurant_id);

alter table meal drop constraint fk_meal_address;
drop index if exists idx_meal_address;
alter table meal drop column address_id;
alter table meal drop column restaurant_name;
