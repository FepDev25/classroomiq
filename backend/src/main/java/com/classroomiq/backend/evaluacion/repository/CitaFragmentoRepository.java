package com.classroomiq.backend.evaluacion.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.evaluacion.domain.CitaFragmento;

/**
 * Citas textuales de un criterio evaluado. Se gestionan en cascada vía el agregado
 * {@code EvaluacionCriterio}; este repo permite consultarlas para el resaltado en la revisión.
 * Aislamiento por tenant vía {@code @TenantId}.
 */
public interface CitaFragmentoRepository extends JpaRepository<CitaFragmento, UUID> {

    List<CitaFragmento> findAllByEvaluacionCriterioIdOrderByOrdenAsc(UUID evaluacionCriterioId);
}
