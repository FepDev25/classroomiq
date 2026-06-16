package com.classroomiq.backend.similitud.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.similitud.domain.ParSimilitud;

/**
 * Pares de similitud de un reporte. Aislamiento por tenant garantizado por {@code @TenantId}.
 * El listado ordenado descendente por similitud semántica alimenta el reporte del lote
 * (pares más similares primero).
 */
public interface ParSimilitudRepository extends JpaRepository<ParSimilitud, UUID> {

    List<ParSimilitud> findAllByReporteIdOrderBySimilitudSemanticaDesc(UUID reporteId);
}
