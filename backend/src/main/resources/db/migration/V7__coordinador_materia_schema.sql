-- Asignación de materias a coordinadores (Fase 5, Hito 6). El admin asigna materias a un usuario
-- con rol COORDINADOR; el coordinador obtiene acceso de SOLO LECTURA a los reportes agregados de
-- esas materias (resúmenes por grupo + narrativa) — nunca a evaluaciones ni trabajos individuales.
-- tenant_id en toda tabla para el aislamiento multi-tenant (estampado por @TenantId).

create table coordinador_materia (
    id             uuid        primary key,
    tenant_id      uuid        not null references institucion (id),
    coordinador_id uuid        not null references usuario (id) on delete cascade,
    materia_id     uuid        not null references materia (id) on delete cascade,
    created_at     timestamptz not null,
    updated_at     timestamptz not null,
    constraint uq_coordmat unique (coordinador_id, materia_id)
);
create index ix_coordmat_tenant on coordinador_materia (tenant_id);
create index ix_coordmat_coordinador on coordinador_materia (coordinador_id);
create index ix_coordmat_materia on coordinador_materia (materia_id);
