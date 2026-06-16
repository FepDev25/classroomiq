package com.classroomiq.backend.similitud.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.similitud.domain.ReporteSimilitud;

/**
 * Reportes de similitud por lote. Aislamiento por tenant garantizado por {@code @TenantId}; los
 * métodos con {@code docenteId} aplican el aislamiento por docente dentro del mismo tenant.
 *
 * <p>{@code findByLoteId} también sirve para saber si un lote ya fue analizado (existe fila).
 * {@code deleteByLoteId} permite reanalizar un lote desde cero (borra pares y fragmentos en cascada).
 */
public interface ReporteSimilitudRepository extends JpaRepository<ReporteSimilitud, UUID> {

    Optional<ReporteSimilitud> findByLoteId(UUID loteId);

    Optional<ReporteSimilitud> findByIdAndDocenteId(UUID id, UUID docenteId);

    boolean existsByLoteId(UUID loteId);

    @Transactional
    void deleteByLoteId(UUID loteId);
}
