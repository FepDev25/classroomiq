-- Registro de uso del LLM. Cada llamada al modelo (evaluación por criterio,
-- narrativa de grupo) deja una fila con los tokens consumidos; es el libro mayor del que el portal
-- admin deriva uso y costo estimado por docente y por mes. El costo NO se persiste: se calcula
-- on-read desde la tarifa configurable (los tokens son el hecho inmutable, el precio es estimación).
-- created_at (de la superclase auditable) es el instante del consumo y la clave de agregación mensual.
-- tenant_id en toda tabla para el aislamiento multi-tenant (estampado por @TenantId).
--
-- entrega_id / lote_id son trazabilidad opcional (on delete set null): si la entrega o el lote se
-- borran, el costo ya gastado debe sobrevivir en el libro mayor — solo se suelta la referencia.

create table registro_uso_llm (
    id            uuid        primary key,
    tenant_id     uuid        not null references institucion (id),
    docente_id    uuid        not null references usuario (id),
    operacion     varchar(20) not null,
    tier          varchar(20) not null,
    modelo        varchar(100) not null,
    input_tokens  bigint      not null,
    output_tokens bigint      not null,
    entrega_id    uuid        references entrega (id) on delete set null,
    lote_id       uuid        references lote (id) on delete set null,
    created_at    timestamptz not null,
    updated_at    timestamptz not null
);
create index ix_usollm_tenant on registro_uso_llm (tenant_id);
create index ix_usollm_docente_fecha on registro_uso_llm (docente_id, created_at);
