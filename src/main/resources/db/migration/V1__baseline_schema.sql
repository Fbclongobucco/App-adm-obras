-- Baseline do schema, gerado a partir das entidades JPA (dialeto PostgreSQL) e revisado.
-- Valido em PostgreSQL e em H2 com MODE=PostgreSQL.
--
-- Colunas de enum (employee.role, meal.meal_type, user_role.role) sao varchar sem CHECK de
-- valores: no H2 2.4.240, um CHECK com dois ou mais valores passa a falhar com
-- "Check constraint invalid" assim que a conexao que criou a tabela e fechada. Os valores
-- sao garantidos pelos enums Java mapeados com @Enumerated(STRING).

create table address (
    id           uuid         not null,
    street       varchar(255) not null,
    number       varchar(255) not null,
    city         varchar(255) not null,
    state        varchar(2)   not null,
    country      varchar(255) not null,
    neighborhood varchar(255) not null,
    zip_code     varchar(8)   not null,
    constraint pk_address primary key (id)
);

create table cost_center (
    id   uuid         not null,
    name varchar(255) not null,
    cnpj varchar(14)  not null,
    constraint pk_cost_center primary key (id)
);

create table client (
    id         uuid         not null,
    name       varchar(255) not null,
    email      varchar(255) not null,
    phone      varchar(255),
    address_id uuid         not null,
    constraint pk_client primary key (id),
    constraint uk_client_email unique (email),
    constraint fk_client_address foreign key (address_id) references address (id)
);

create table employee (
    id             uuid         not null,
    name           varchar(255) not null,
    email          varchar(255) not null,
    cpf            varchar(11)  not null,
    phone          varchar(11),
    birth_date     date         not null,
    role           varchar(255) not null,
    address_id     uuid,
    cost_center_id uuid         not null,
    constraint pk_employee primary key (id),
    constraint uk_employee_email unique (email),
    constraint uk_employee_cpf unique (cpf),
    constraint fk_employee_address foreign key (address_id) references address (id),
    constraint fk_employee_cost_center foreign key (cost_center_id) references cost_center (id)
);

create table project (
    id             uuid           not null,
    os             varchar(255)   not null,
    description    varchar(500),
    start_date     date           not null,
    end_date       date,
    is_completed   boolean        not null,
    total_price    numeric(19, 2) not null,
    client_id      uuid,
    cost_center_id uuid,
    constraint pk_project primary key (id),
    constraint uk_project_os unique (os),
    constraint fk_project_client foreign key (client_id) references client (id),
    constraint fk_project_cost_center foreign key (cost_center_id) references cost_center (id)
);

create table accommodation (
    id          uuid           not null,
    host_name   varchar(255)   not null,
    host_phone  varchar(11)    not null,
    capacity    integer        not null,
    days        integer        not null,
    is_contract boolean        not null,
    total_price numeric(19, 2) not null,
    address_id  uuid           not null,
    project_id  uuid,
    constraint pk_accommodation primary key (id),
    constraint fk_accommodation_address foreign key (address_id) references address (id),
    constraint fk_accommodation_project foreign key (project_id) references project (id)
);

create table meal (
    id              uuid           not null,
    restaurant_name varchar(255)   not null,
    meal_type       varchar(255)   not null,
    date            date           not null,
    price           numeric(19, 2) not null,
    quantity        integer        not null,
    total_price     numeric(19, 2) not null,
    is_billed       boolean        not null,
    address_id      uuid,
    project_id      uuid,
    constraint pk_meal primary key (id),
    -- Um projeto tem no maximo uma refeicao de cada tipo; project_id nulo nao conflita
    constraint uk_meal_project_type unique (project_id, meal_type),
    constraint fk_meal_address foreign key (address_id) references address (id),
    constraint fk_meal_project foreign key (project_id) references project (id)
);

create table project_employee (
    project_id  uuid not null,
    employee_id uuid not null,
    constraint pk_project_employee primary key (employee_id, project_id),
    constraint fk_project_employee_project foreign key (project_id) references project (id),
    constraint fk_project_employee_employee foreign key (employee_id) references employee (id)
);

create table accommodation_employee (
    accommodation_id uuid not null,
    employee_id      uuid not null,
    constraint pk_accommodation_employee primary key (accommodation_id, employee_id),
    constraint fk_accommodation_employee_accommodation foreign key (accommodation_id) references accommodation (id),
    constraint fk_accommodation_employee_employee foreign key (employee_id) references employee (id)
);

create table app_user (
    id            uuid         not null,
    name          varchar(255) not null,
    email         varchar(255) not null,
    is_active     boolean      not null,
    password_hash varchar(100) not null,
    constraint pk_app_user primary key (id),
    constraint uk_app_user_email unique (email)
);

create table user_role (
    user_id uuid         not null,
    role    varchar(255) not null,
    constraint pk_user_role primary key (user_id, role),
    constraint fk_user_role_user foreign key (user_id) references app_user (id)
);

-- Indices nas FKs consultadas. meal.project_id ja e coberto por uk_meal_project_type,
-- e as chaves primarias das juncoes cobrem a primeira coluna de cada uma.
create index idx_client_address on client (address_id);
create index idx_employee_cost_center on employee (cost_center_id);
create index idx_employee_address on employee (address_id);
create index idx_project_client on project (client_id);
create index idx_project_cost_center on project (cost_center_id);
create index idx_accommodation_project on accommodation (project_id);
create index idx_accommodation_address on accommodation (address_id);
create index idx_meal_address on meal (address_id);
create index idx_project_employee_project on project_employee (project_id);
create index idx_accommodation_employee_employee on accommodation_employee (employee_id);
