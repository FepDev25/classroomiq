package com.classroomiq.backend.metricas.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.classroomiq.backend.metricas.domain.RegistroUsoLlm;

/**
 * Libro mayor de uso del LLM (Fase 6). Aislamiento por tenant vía {@code @TenantId}: las consultas
 * del admin agregan solo el uso de su institución (el discriminador se aplica también a estas
 * consultas JPQL). El rango {@code [desde, hasta)} acota el mes; el costo se calcula on-read fuera
 * de la consulta a partir de los tokens agregados.
 */
public interface RegistroUsoLlmRepository extends JpaRepository<RegistroUsoLlm, UUID> {

    /** Tokens consumidos por docente y modelo en el rango, para el resumen de uso/costo por docente. */
    @Query("""
            select r.docenteId as docenteId, r.modelo as modelo,
                   sum(r.inputTokens) as inputTokens, sum(r.outputTokens) as outputTokens
            from RegistroUsoLlm r
            where r.createdAt >= :desde and r.createdAt < :hasta
            group by r.docenteId, r.modelo
            """)
    List<UsoDocenteModelo> agregarPorDocenteModelo(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /** Desglose de un docente por modelo y operación en el rango, para el detalle del portal admin. */
    @Query("""
            select r.modelo as modelo, r.operacion as operacion,
                   sum(r.inputTokens) as inputTokens, sum(r.outputTokens) as outputTokens
            from RegistroUsoLlm r
            where r.docenteId = :docenteId and r.createdAt >= :desde and r.createdAt < :hasta
            group by r.modelo, r.operacion
            """)
    List<UsoModeloOperacion> agregarPorModeloOperacion(@Param("docenteId") UUID docenteId,
            @Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
