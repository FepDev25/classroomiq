package com.classroomiq.backend.evaluacion.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.evaluacion.domain.EvaluacionCriterio;

/**
 * Criterios de un borrador de evaluación. Normalmente se gestionan en cascada vía el agregado
 * {@code Evaluacion}; este repo permite consultarlos/actualizarlos sueltos en la revisión (Fase 4 — H5).
 * Aislamiento por tenant vía {@code @TenantId}.
 */
public interface EvaluacionCriterioRepository extends JpaRepository<EvaluacionCriterio, UUID> {

    List<EvaluacionCriterio> findAllByEvaluacionIdOrderByOrdenAsc(UUID evaluacionId);
}
