package com.classroomiq.backend.entrega.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.entrega.domain.Lote;

/**
 * Aislamiento por tenant garantizado por {@code @TenantId}; los métodos con {@code docenteId}
 * aplican el aislamiento por docente dentro del mismo tenant.
 */
public interface LoteRepository extends JpaRepository<Lote, UUID> {

    List<Lote> findAllByDocenteId(UUID docenteId);

    Optional<Lote> findByIdAndDocenteId(UUID id, UUID docenteId);

    List<Lote> findAllByMateriaId(UUID materiaId);
}
