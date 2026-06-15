-- Esquema de Fase 3: lotes de entregas, entregas, archivos y fragmentos vectorizados.
-- tenant_id en toda tabla para el aislamiento multi-tenant (estampado por @TenantId).
-- docente_id/materia_id/lote_id se replican en fragmento_entrega como metadatos para acotar
-- la búsqueda de similitud al mismo docente, materia y lote (Fase 5).

create table lote (
    id         uuid         primary key,
    tenant_id  uuid         not null references institucion (id),
    docente_id uuid         not null references usuario (id),
    materia_id uuid         not null references materia (id),
    rubrica_id uuid         not null references rubrica (id),
    nombre     varchar(255) not null,
    estado     varchar(20)  not null,
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    constraint ck_lote_estado check (estado in ('ABIERTO', 'PROCESANDO', 'LISTO'))
);
create index ix_lote_tenant on lote (tenant_id);
create index ix_lote_docente on lote (docente_id);
create index ix_lote_materia on lote (materia_id);

create table entrega (
    id                       uuid         primary key,
    tenant_id                uuid         not null references institucion (id),
    docente_id               uuid         not null references usuario (id),
    materia_id               uuid         not null references materia (id),
    lote_id                  uuid         not null references lote (id) on delete cascade,
    identificador_estudiante varchar(255) not null,
    tipo                     varchar(20)  not null,
    estado                   varchar(20)  not null,
    mensaje_error            varchar(4000),
    created_at               timestamptz  not null,
    updated_at               timestamptz  not null,
    constraint ck_entrega_tipo check (tipo in ('DOCUMENTO', 'CODIGO', 'MIXTA')),
    constraint ck_entrega_estado check (estado in ('PENDIENTE', 'PROCESANDO', 'EVALUANDO', 'LISTO', 'ERROR'))
);
create index ix_entrega_tenant on entrega (tenant_id);
create index ix_entrega_docente on entrega (docente_id);
create index ix_entrega_lote on entrega (lote_id);

create table archivo_entrega (
    id              uuid          primary key,
    tenant_id       uuid          not null references institucion (id),
    entrega_id      uuid          not null references entrega (id) on delete cascade,
    nombre_original varchar(512)  not null,
    ruta_relativa   varchar(1024) not null,
    mime_type       varchar(255),
    tamano_bytes    bigint        not null,
    hash_sha256     varchar(64),
    rol             varchar(20)   not null,
    orden           integer       not null default 0,
    created_at      timestamptz   not null,
    updated_at      timestamptz   not null,
    constraint ck_archivo_rol check (rol in ('DOCUMENTO', 'CODIGO'))
);
create index ix_archivo_tenant on archivo_entrega (tenant_id);
create index ix_archivo_entrega on archivo_entrega (entrega_id);

create table fragmento_entrega (
    id           uuid         primary key,
    tenant_id    uuid         not null references institucion (id),
    docente_id   uuid         not null references usuario (id),
    materia_id   uuid         not null references materia (id),
    lote_id      uuid         not null references lote (id) on delete cascade,
    entrega_id   uuid         not null references entrega (id) on delete cascade,
    archivo_id   uuid         references archivo_entrega (id) on delete set null,
    orden        integer      not null default 0,
    contenido    text         not null,
    seccion      varchar(512),
    linea_inicio integer,
    linea_fin    integer,
    embedding    vector(1024) not null,
    created_at   timestamptz  not null,
    updated_at   timestamptz  not null
);
create index ix_fragmento_tenant on fragmento_entrega (tenant_id);
create index ix_fragmento_entrega on fragmento_entrega (entrega_id);
create index ix_fragmento_lote on fragmento_entrega (lote_id);

-- Índice HNSW para búsqueda de similitud coseno (Fase 5). El operador <=> calcula la distancia
-- coseno; como los embeddings están normalizados L2, equivale a 1 - similitud coseno.
create index ix_fragmento_embedding_hnsw on fragmento_entrega
    using hnsw (embedding vector_cosine_ops);
