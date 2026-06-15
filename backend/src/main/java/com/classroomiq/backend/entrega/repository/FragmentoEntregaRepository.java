package com.classroomiq.backend.entrega.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.entrega.domain.FragmentoEntrega;

/**
 * Fragmentos vectorizados. Aislamiento por tenant garantizado por {@code @TenantId}.
 * El borrado por entrega permite reprocesar una entrega desde cero (Hito 5).
 * Las consultas de similitud coseno se añaden en la Fase 5.
 */
public interface FragmentoEntregaRepository extends JpaRepository<FragmentoEntrega, UUID> {

    List<FragmentoEntrega> findAllByEntregaIdOrderByOrdenAsc(UUID entregaId);

    long countByEntregaId(UUID entregaId);

    @Transactional
    void deleteByEntregaId(UUID entregaId);
}
