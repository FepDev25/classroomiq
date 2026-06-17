package com.classroomiq.backend.coordinador.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.coordinador.domain.AsignacionCoordinador;

/**
 * Asignaciones materia↔coordinador. Aislamiento por tenant vía {@code @TenantId}. Las consultas por
 * {@code coordinadorId} acotan el acceso del coordinador a sus materias asignadas dentro del tenant.
 */
public interface AsignacionCoordinadorRepository extends JpaRepository<AsignacionCoordinador, UUID> {

    List<AsignacionCoordinador> findAllByCoordinadorId(UUID coordinadorId);

    Optional<AsignacionCoordinador> findByCoordinadorIdAndMateriaId(UUID coordinadorId, UUID materiaId);

    boolean existsByCoordinadorIdAndMateriaId(UUID coordinadorId, UUID materiaId);
}
