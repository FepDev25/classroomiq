-- Baseline del esquema.
-- Habilita pgvector para los embeddings de similitud semántica (se usa desde Fase 3).
-- Las tablas del dominio se crean en migraciones posteriores (Hito 1).
CREATE EXTENSION IF NOT EXISTS vector;
