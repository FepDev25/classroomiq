-- Narrativa del resumen por grupo (Fase 5, Hito 5). Las estadísticas del resumen se computan
-- on-demand sobre las evaluaciones aprobadas (no se persisten); lo único que se guarda es el TEXTO
-- narrativo generado por LLM, que es caro de regenerar. 1:1 con el lote; regenerar lo sobrescribe.
-- tenant_id en toda tabla para el aislamiento multi-tenant (estampado por @TenantId).

create table resumen_grupo (
    id          uuid          primary key,
    tenant_id   uuid          not null references institucion (id),
    docente_id  uuid          not null references usuario (id),
    lote_id     uuid          not null references lote (id) on delete cascade,
    narrativa   text          not null,
    modelo      varchar(100),
    generado_at timestamptz   not null,
    created_at  timestamptz   not null,
    updated_at  timestamptz   not null,
    constraint uq_resgrupo_lote unique (lote_id)
);
create index ix_resgrupo_tenant on resumen_grupo (tenant_id);
create index ix_resgrupo_docente on resumen_grupo (docente_id);
