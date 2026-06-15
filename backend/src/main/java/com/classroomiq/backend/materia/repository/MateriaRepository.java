package com.classroomiq.backend.materia.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.materia.domain.Materia;

/**
 * Hibernate ya garantiza el aislamiento por tenant (vía {@code @TenantId}). Los métodos
 * con {@code docenteId} aplican además el aislamiento por docente dentro del mismo tenant.
 */
public interface MateriaRepository extends JpaRepository<Materia, UUID> {

    List<Materia> findAllByDocenteId(UUID docenteId);

    Optional<Materia> findByIdAndDocenteId(UUID id, UUID docenteId);
}
