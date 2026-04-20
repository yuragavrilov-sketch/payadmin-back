-- Платёж: схема админ-панели оператора
-- Хранит ТОЛЬКО операторские данные (audit, заметки, сохранённые виды).
-- Бизнес-данные (транзакции, мерчанты, споры) живут в core-БД платёжки.
--
-- Схема создаётся Flyway при первом запуске (spring.flyway.create-schemas=true,
-- имя в spring.flyway.schemas), поэтому тут без CREATE SCHEMA — всё создастся
-- внутри неё. gen_random_uuid() встроен в Postgres 13+, extension pgcrypto
-- не нужен (избегаем прав superuser).

-- Кэш профиля оператора из Keycloak.
-- keycloak_sub — это `sub` из JWT, неизменный для пользователя.
create table operator (
    id              uuid primary key default gen_random_uuid(),
    keycloak_sub    text not null unique,
    email           text not null,
    display_name    text not null,
    role            text not null default 'operator',
    created_at      timestamptz not null default now(),
    last_seen_at    timestamptz
);

create index operator_email_idx on operator (email);

-- Журнал действий оператора. Иммутабельный — только append.
-- payload содержит снапшот «до/после» в свободной форме.
create table audit_event (
    id              bigserial primary key,
    operator_id     uuid not null references operator(id),
    action          text not null,
    entity_type     text not null,
    entity_id       text not null,
    payload         jsonb not null default '{}'::jsonb,
    ip              inet,
    user_agent      text,
    created_at      timestamptz not null default now()
);

create index audit_event_operator_idx on audit_event (operator_id, created_at desc);
create index audit_event_entity_idx   on audit_event (entity_type, entity_id, created_at desc);
create index audit_event_created_idx  on audit_event (created_at desc);

-- Внутренние комментарии операторов к сущностям (мерчант / транзакция / спор).
create table internal_note (
    id              uuid primary key default gen_random_uuid(),
    operator_id     uuid not null references operator(id),
    entity_type     text not null,
    entity_id       text not null,
    body            text not null,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

create index internal_note_entity_idx on internal_note (entity_type, entity_id, created_at desc);

-- Сохранённые фильтры/виды на страницах списков.
create table saved_view (
    id              uuid primary key default gen_random_uuid(),
    operator_id     uuid not null references operator(id),
    page            text not null,
    name            text not null,
    filters         jsonb not null default '{}'::jsonb,
    is_default      boolean not null default false,
    created_at      timestamptz not null default now(),
    constraint saved_view_unique_name unique (operator_id, page, name)
);

create index saved_view_operator_page_idx on saved_view (operator_id, page);

-- Только один дефолтный вид на страницу у одного оператора.
create unique index saved_view_one_default_per_page
    on saved_view (operator_id, page)
    where is_default;
