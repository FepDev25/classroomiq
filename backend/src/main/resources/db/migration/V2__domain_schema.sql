-- Esquema de dominio de Fase 2: instituciones (tenant), usuarios, materias,
-- rúbricas, criterios y niveles de desempeño.
-- Implementa data/rubricas-ejemplo/SCHEMA.md: puntos absolutos por criterio,
-- modo de total (suma/promedio) y tres formas de puntaje por nivel.
-- tenant_id en toda tabla para el aislamiento multi-tenant (filtro en Hito 2).

create table institucion (
    id         uuid         primary key,
    nombre     varchar(255) not null,
    activa     boolean      not null default true,
    created_at timestamptz  not null,
    updated_at timestamptz  not null
);

create table usuario (
    id            uuid         primary key,
    tenant_id     uuid         not null references institucion (id),
    email         varchar(320) not null,
    password_hash varchar(100) not null,
    nombre        varchar(255) not null,
    rol           varchar(20)  not null,
    activo        boolean      not null default true,
    ultimo_acceso timestamptz,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    constraint uq_usuario_email unique (email),
    constraint ck_usuario_rol check (rol in ('ADMIN', 'DOCENTE', 'COORDINADOR'))
);
create index ix_usuario_tenant on usuario (tenant_id);

create table materia (
    id                uuid          primary key,
    tenant_id         uuid          not null references institucion (id),
    docente_id        uuid          not null references usuario (id),
    nombre            varchar(255)  not null,
    periodo_academico varchar(100),
    descripcion       varchar(4000),
    archivada         boolean       not null default false,
    created_at        timestamptz   not null,
    updated_at        timestamptz   not null
);
create index ix_materia_tenant on materia (tenant_id);
create index ix_materia_docente on materia (docente_id);

create table rubrica (
    id            uuid          primary key,
    tenant_id     uuid          not null references institucion (id),
    docente_id    uuid          not null references usuario (id),
    materia_id    uuid          not null references materia (id),
    nombre        varchar(255)  not null,
    descripcion   varchar(4000),
    puntaje_total numeric(6, 2) not null,
    modo_total    varchar(20)   not null,
    created_at    timestamptz   not null,
    updated_at    timestamptz   not null,
    constraint ck_rubrica_modo check (modo_total in ('SUMA', 'PROMEDIO'))
);
create index ix_rubrica_tenant on rubrica (tenant_id);
create index ix_rubrica_docente on rubrica (docente_id);
create index ix_rubrica_materia on rubrica (materia_id);

create table criterio (
    id                      uuid          primary key,
    tenant_id               uuid          not null references institucion (id),
    rubrica_id              uuid          not null references rubrica (id) on delete cascade,
    nombre                  varchar(255)  not null,
    descripcion             varchar(4000),
    puntaje_maximo          numeric(6, 2) not null,
    evaluable_por_contenido boolean       not null default true,
    orden                   integer       not null default 0,
    created_at              timestamptz   not null,
    updated_at              timestamptz   not null
);
create index ix_criterio_tenant on criterio (tenant_id);
create index ix_criterio_rubrica on criterio (rubrica_id);

create table nivel_desempeno (
    id            uuid          primary key,
    tenant_id     uuid          not null references institucion (id),
    criterio_id   uuid          not null references criterio (id) on delete cascade,
    nombre        varchar(255)  not null,
    descripcion   varchar(4000),
    tipo_puntaje  varchar(20)   not null,
    puntaje_min   numeric(6, 2),
    puntaje_max   numeric(6, 2),
    puntaje_valor numeric(6, 2),
    pct_min       numeric(5, 2),
    pct_max       numeric(5, 2),
    orden         integer       not null default 0,
    created_at    timestamptz   not null,
    updated_at    timestamptz   not null,
    constraint ck_nivel_tipo check (tipo_puntaje in ('RANGO', 'FIJO', 'BANDA_PCT'))
);
create index ix_nivel_tenant on nivel_desempeno (tenant_id);
create index ix_nivel_criterio on nivel_desempeno (criterio_id);
