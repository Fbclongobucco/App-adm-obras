-- Refresh tokens da autenticacao stateless (change autenticacao-stateless-jwt).
-- Guarda so o hash do token; apagar o usuario apaga os tokens dele.
-- revoke_reason sem CHECK de valores pelo mesmo motivo descrito no V1.

create table refresh_token (
    id            uuid                        not null,
    user_id       uuid                        not null,
    token_hash    varchar(64)                 not null,
    family_id     uuid                        not null,
    expires_at    timestamp(6) with time zone not null,
    created_at    timestamp(6) with time zone not null,
    revoked_at    timestamp(6) with time zone,
    revoke_reason varchar(20),
    constraint pk_refresh_token primary key (id),
    constraint uk_refresh_token_hash unique (token_hash),
    constraint fk_refresh_token_user foreign key (user_id) references app_user (id) on delete cascade
);

create index idx_refresh_token_family on refresh_token (family_id);
create index idx_refresh_token_user on refresh_token (user_id);
