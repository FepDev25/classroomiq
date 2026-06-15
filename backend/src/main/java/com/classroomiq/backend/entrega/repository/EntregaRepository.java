package com.classroomiq.backend.entrega.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.entrega.domain.Entrega;

/**
 * Aislamiento por tenant garantizado por {@code @TenantId}; los métodos con {@code docenteId}
 * aplican el aislamiento por docente dentro del mismo tenant.
 */
public interface EntregaRepository extends JpaRepository<Entrega, UUID> {

    List<Entrega> findAllByLoteId(UUID loteId);

    List<Entrega> findAllByDocenteId(UUID docenteId);

    Optional<Entrega> findByIdAndDocenteId(UUID id, UUID docenteId);
}
