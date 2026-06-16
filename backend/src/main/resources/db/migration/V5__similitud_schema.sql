-- Reporte de similitud por lote (persistido).
-- El reporte se calcula UNA vez sobre las entregas LISTO del lote y se guarda: recomputarlo es
-- caro (pares O(n^2) + embeddings) y el resultado es estable. La existencia de la fila
-- reporte_similitud indica que el lote ya fue analizado.
--
-- Dos niveles de similitud por par (CLAUDE.md sección 5):
--   - semántica: coseno de los embeddings agregados (centroide) por entrega, vía pgvector.
--   - textual: solapamiento de n-gramas de palabras (fragmentos copiados literalmente).
-- Output SIEMPRE como alerta para revisión manual, nunca como acusación de deshonestidad.
--
-- Acotado a un solo lote (mismo docente y materia). tenant_id en toda tabla para el aislamiento
-- multi-tenant (estampado por @TenantId); el SQL nativo de pgvector lo filtra explícitamente.

create table reporte_similitud (
    id          uuid          primary key,
    tenant_id   uuid          not null references institucion (id),
    docente_id  uuid          not null references usuario (id),
    materia_id  uuid          not null references materia (id),
    lote_id     uuid          not null references lote (id) on delete cascade,
    umbral      numeric(5, 4) not null,
    generado_at timestamptz   not null,
    created_at  timestamptz   not null,
    updated_at  timestamptz   not null,
    constraint uq_repsim_lote unique (lote_id)
);
create index ix_repsim_tenant on reporte_similitud (tenant_id);
create index ix_repsim_docente on reporte_similitud (docente_id);

-- Un registro por par no ordenado de entregas del lote (entrega_a_id < entrega_b_id por
-- convención del cálculo). similitud_textual queda nula si la entrega no tiene prosa comparable.
create table par_similitud (
    id                  uuid          primary key,
    tenant_id           uuid          not null references institucion (id),
    reporte_id          uuid          not null references reporte_similitud (id) on delete cascade,
    entrega_a_id        uuid          not null references entrega (id) on delete cascade,
    entrega_b_id        uuid          not null references entrega (id) on delete cascade,
    similitud_semantica numeric(5, 4) not null,
    similitud_textual   numeric(5, 4),
    supera_umbral       boolean       not null default false,
    created_at          timestamptz   not null,
    updated_at          timestamptz   not null
);
create index ix_parsim_tenant on par_similitud (tenant_id);
create index ix_parsim_reporte on par_similitud (reporte_id);

-- Fragmentos concretos donde la similitud del par es mayor, para la visualización lado a lado.
-- fragmento_a_id/b_id referencian el chunk de origen (nulo si se reindexa); texto_a/b guardan
-- la cita textual exacta. tipo distingue el origen de la coincidencia (semántica o textual).
create table fragmento_par_similar (
    id             uuid          primary key,
    tenant_id      uuid          not null references institucion (id),
    par_id         uuid          not null references par_similitud (id) on delete cascade,
    tipo           varchar(20)   not null,
    fragmento_a_id uuid          references fragmento_entrega (id) on delete set null,
    fragmento_b_id uuid          references fragmento_entrega (id) on delete set null,
    texto_a        text          not null,
    texto_b        text          not null,
    similitud      numeric(5, 4) not null,
    orden          integer       not null default 0,
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    constraint ck_fragparsim_tipo check (tipo in ('SEMANTICA', 'TEXTUAL'))
);
create index ix_fragparsim_tenant on fragmento_par_similar (tenant_id);
create index ix_fragparsim_par on fragmento_par_similar (par_id);
