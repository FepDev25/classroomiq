package com.classroomiq.backend.coordinador.domain;

import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Asignación de una materia a un coordinador (Fase 5, Hito 6). El admin la crea; le da al
 * coordinador acceso de solo lectura a los reportes agregados de esa materia. Par único
 * {@code (coordinadorId, materiaId)}.
 */
@Entity
@Table(name = "coordinador_materia")
@Getter
@Setter
public class AsignacionCoordinador extends AbstractTenantEntity {

    @Column(name = "coordinador_id", nullable = false)
    private UUID coordinadorId;

    @Column(name = "materia_id", nullable = false)
    private UUID materiaId;
}
