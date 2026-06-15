-- Borradores de evaluación generados por el LLM y revisados por el docente.
-- Una evaluacion por entrega (1:1). El motor genera el borrador por criterio; el docente lo edita
-- y aprueba. La EXISTENCIA de la fila evaluacion distingue "entrega indexada (Fase 3, estado LISTO)"
-- de "entrega evaluada (con borrador)", sin inflar el enum estado de entrega.
-- tenant_id en toda tabla para el aislamiento multi-tenant (estampado por @TenantId).

create table evaluacion (
    id                     uuid          primary key,
    tenant_id              uuid          not null references institucion (id),
    docente_id             uuid          not null references usuario (id),
    entrega_id             uuid          not null references entrega (id) on delete cascade,
    rubrica_id             uuid          not null references rubrica (id),
    estado                 varchar(20)   not null,
    puntaje_total_sugerido numeric(7, 2),
    puntaje_total_final    numeric(7, 2),
    comentario_general     varchar(4000),
    aprobada_at            timestamptz,
    created_at             timestamptz   not null,
    updated_at             timestamptz   not null,
    constraint uq_evaluacion_entrega unique (entrega_id),
    constraint ck_evaluacion_estado check (estado in ('BORRADOR', 'APROBADA'))
);
create index ix_evaluacion_tenant on evaluacion (tenant_id);
create index ix_evaluacion_docente on evaluacion (docente_id);

-- Un registro por criterio de la rúbrica. Los criterios con evaluable=false
-- (evaluablePorContenido=false: demo, exposición) los deja el motor en blanco para el docente.
create table evaluacion_criterio (
    id                    uuid          primary key,
    tenant_id             uuid          not null references institucion (id),
    evaluacion_id         uuid          not null references evaluacion (id) on delete cascade,
    criterio_id           uuid          not null references criterio (id),
    evaluable             boolean       not null default true,
    nivel_sugerido_id     uuid          references nivel_desempeno (id) on delete set null,
    puntaje_sugerido      numeric(6, 2),
    justificacion         text,
    advertencia           text,
    nivel_final_id        uuid          references nivel_desempeno (id) on delete set null,
    puntaje_final         numeric(6, 2),
    justificacion_editada text,
    revisado_manual       boolean       not null default false,
    orden                 integer       not null default 0,
    created_at            timestamptz   not null,
    updated_at            timestamptz   not null
);
create index ix_evalcrit_tenant on evaluacion_criterio (tenant_id);
create index ix_evalcrit_evaluacion on evaluacion_criterio (evaluacion_id);
create index ix_evalcrit_criterio on evaluacion_criterio (criterio_id);

-- Fragmentos del trabajo citados por el LLM como evidencia de cada criterio, para el resaltado
-- en la pantalla de revisión. fragmento_id referencia el chunk de origen (puede quedar nulo si el
-- fragmento se reindexa); texto_citado guarda la cita textual exacta usada en la justificación.
create table cita_fragmento (
    id                     uuid         primary key,
    tenant_id              uuid         not null references institucion (id),
    evaluacion_criterio_id uuid         not null references evaluacion_criterio (id) on delete cascade,
    fragmento_id           uuid         references fragmento_entrega (id) on delete set null,
    texto_citado           text         not null,
    orden                  integer      not null default 0,
    created_at             timestamptz  not null,
    updated_at             timestamptz  not null
);
create index ix_cita_tenant on cita_fragmento (tenant_id);
create index ix_cita_evalcrit on cita_fragmento (evaluacion_criterio_id);
