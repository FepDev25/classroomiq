package com.classroomiq.backend.rubrica.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.rubrica.domain.Rubrica;

/**
 * Aislamiento por tenant garantizado por {@code @TenantId}; los métodos con {@code docenteId}
 * aplican el aislamiento por docente dentro del mismo tenant.
 */
public interface RubricaRepository extends JpaRepository<Rubrica, UUID> {

    List<Rubrica> findAllByDocenteId(UUID docenteId);

    Optional<Rubrica> findByIdAndDocenteId(UUID id, UUID docenteId);

    List<Rubrica> findAllByMateriaId(UUID materiaId);
}
