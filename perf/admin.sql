-- Usuario ADMIN do teste de carga (alvo perf). O perfil prod nunca roda o bootstrap.
-- Login: perf-admin@perf.test / perf-admin-123. Hash BCrypt gerado com o BCryptPasswordEncoder da aplicacao.
-- Idempotente. Nao usar em producao.
insert into app_user (id, name, email, is_active, password_hash, created_at, updated_at)
values ('00000000-0000-0000-0000-00000000a001', 'Administrador de carga', 'perf-admin@perf.test', true,
        '$2a$10$u7bqUQQDW0XLRO8OzIWSNuOBbzLBNr0iQxHt9jUvoxZD7ZYICJP4W', current_timestamp, current_timestamp)
on conflict (id) do nothing;

insert into user_role (user_id, role)
values ('00000000-0000-0000-0000-00000000a001', 'ADMIN')
on conflict do nothing;
