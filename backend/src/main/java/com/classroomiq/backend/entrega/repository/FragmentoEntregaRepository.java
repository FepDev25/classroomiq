package com.classroomiq.backend.entrega.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.entrega.domain.FragmentoEntrega;

/**
 * Fragmentos vectorizados. Aislamiento por tenant garantizado por {@code @TenantId} en las
 * consultas derivadas; la búsqueda por similitud usa SQL nativo (que NO aplica el discriminador),
 * por lo que filtra el {@code tenant_id} de forma explícita.
 * El borrado por entrega permite reprocesar una entrega desde cero (Hito 5 de la Fase 3).
 */
public interface FragmentoEntregaRepository extends JpaRepository<FragmentoEntrega, UUID> {

    List<FragmentoEntrega> findAllByEntregaIdOrderByOrdenAsc(UUID entregaId);

    long countByEntregaId(UUID entregaId);

    @Transactional
    void deleteByEntregaId(UUID entregaId);

    /**
     * Recupera los {@code k} fragmentos de una entrega más cercanos al vector de consulta por
     * distancia coseno ({@code <=>}), de menor a mayor distancia. Es el retrieval semántico por
     * criterio del motor de evaluación (Fase 4, RAG por criterio).
     *
     * <p>Query nativa: el operador {@code <=>} aprovecha el índice HNSW ({@code vector_cosine_ops})
     * para el {@code order by ... limit k}. Como el SQL nativo evita el filtro de {@code @TenantId},
     * el {@code tenant_id} se acota explícitamente — además del {@code entrega_id} — para no cruzar
     * datos entre instituciones. El vector se pasa como {@code float[]} y se castea a {@code vector}.
     */
    @Query(value = """
            select f.id           as id,
                   f.archivo_id   as archivoId,
                   f.contenido    as contenido,
                   f.seccion      as seccion,
                   f.linea_inicio as lineaInicio,
                   f.linea_fin    as lineaFin,
                   f.embedding <=> cast(:consulta as vector) as distancia
            from fragmento_entrega f
            where f.entrega_id = :entregaId
              and f.tenant_id  = :tenantId
            order by f.embedding <=> cast(:consulta as vector)
            limit :k
            """, nativeQuery = true)
    List<FragmentoSimilar> buscarSimilaresEnEntrega(
            @Param("tenantId") UUID tenantId,
            @Param("entregaId") UUID entregaId,
            @Param("consulta") float[] consulta,
            @Param("k") int k);
}
