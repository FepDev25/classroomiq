package com.classroomiq.backend.metricas.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.metricas.domain.RegistroUsoLlm;

/**
 * Libro mayor de uso del LLM (Fase 6). Aislamiento por tenant vía {@code @TenantId}: las consultas
 * del admin ven solo el uso de su institución. Las agregaciones por docente y por mes (uso/costo
 * del portal admin) se añaden en el Hito 3; aquí queda el registro de inserción del consumo.
 */
public interface RegistroUsoLlmRepository extends JpaRepository<RegistroUsoLlm, UUID> {
}
