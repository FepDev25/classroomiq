package com.classroomiq.backend.entrega.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.entrega.domain.ArchivoEntrega;

/**
 * Archivos de una entrega. Normalmente se gestionan vía la cascada del agregado {@link
 * com.classroomiq.backend.entrega.domain.Entrega}; este repositorio permite consultarlos sin
 * navegar la colección lazy. Aislamiento por tenant garantizado por {@code @TenantId}.
 */
public interface ArchivoEntregaRepository extends JpaRepository<ArchivoEntrega, UUID> {

    List<ArchivoEntrega> findAllByEntrega_IdOrderByOrdenAsc(UUID entregaId);
}
