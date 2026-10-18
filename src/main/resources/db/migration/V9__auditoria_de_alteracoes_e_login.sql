-- Auditoria de alteracoes e de login (change desempenho-virtual-threads-e-auditoria).
-- change_audit: quem criou, alterou ou excluiu cada registro de negocio, e quando. Sem valores de campos.
-- login_audit: login com sucesso, com IP e user-agent (dados pessoais: acesso so ADMIN).
-- Sem FK para app_user: o usuario pode ser excluido e o registro permanece. Nome e e-mail sao do instante.
-- entity_type e action sem CHECK de valores pelo mesmo motivo descrito no V1.
-- Ids gerados pela aplicacao. Valido em PostgreSQL e em H2 com MODE=PostgreSQL.

create table change_audit (
    id          uuid                        not null,
    entity_type varchar(30)                 not null,
    entity_id   uuid                        not null,
    action      varchar(10)                 not null,
    occurred_at timestamp(6) with time zone not null,
    actor_id    uuid,
    actor_name  varchar(255)                not null,
    actor_email varchar(255),
    constraint pk_change_audit primary key (id)
);

-- Filtros de GET /api/audit/changes, todos ordenados por occurred_at desc
create index idx_change_audit_entity on change_audit (entity_type, entity_id, occurred_at);
create index idx_change_audit_actor on change_audit (actor_id, occurred_at);
create index idx_change_audit_occurred on change_audit (occurred_at);

create table login_audit (
    id          uuid                        not null,
    user_id     uuid                        not null,
    user_name   varchar(255)                not null,
    user_email  varchar(255)                not null,
    occurred_at timestamp(6) with time zone not null,
    ip_address  varchar(45),
    user_agent  varchar(512),
    constraint pk_login_audit primary key (id)
);

-- Filtros de GET /api/audit/logins
create index idx_login_audit_user on login_audit (user_id, occurred_at);
create index idx_login_audit_occurred on login_audit (occurred_at);
