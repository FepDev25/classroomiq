package com.classroomiq.backend.reportes.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.reportes.domain.ResumenGrupo;

/**
 * Narrativa persistida del resumen por grupo. Aislamiento por tenant vía {@code @TenantId}.
 * {@code findByLoteId} sirve para leer la narrativa (si existe) al construir el resumen y para
 * regenerarla in-place (actualizar la misma fila, sin borrar/insertar).
 */
public interface ResumenGrupoRepository extends JpaRepository<ResumenGrupo, UUID> {

    Optional<ResumenGrupo> findByLoteId(UUID loteId);
}
