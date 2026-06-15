package com.classroomiq.backend.evaluacion.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.evaluacion.domain.Evaluacion;

/**
 * Borradores de evaluación. Aislamiento por tenant garantizado por {@code @TenantId}; los métodos
 * con {@code docenteId} aplican el aislamiento por docente dentro del mismo tenant.
 *
 * <p>{@code findByEntregaId} también sirve para saber si una entrega ya fue evaluada (existe fila).
 * {@code deleteByEntregaId} permite reevaluar una entrega desde cero (borra el borrador previo en
 * cascada: criterios y citas).
 */
public interface EvaluacionRepository extends JpaRepository<Evaluacion, UUID> {

    Optional<Evaluacion> findByEntregaId(UUID entregaId);

    Optional<Evaluacion> findByIdAndDocenteId(UUID id, UUID docenteId);

    List<Evaluacion> findAllByDocenteId(UUID docenteId);

    boolean existsByEntregaId(UUID entregaId);

    @Transactional
    void deleteByEntregaId(UUID entregaId);
}
