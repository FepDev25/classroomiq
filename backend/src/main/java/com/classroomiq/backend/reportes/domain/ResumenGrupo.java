package com.classroomiq.backend.reportes.domain;

import java.time.Instant;
import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Texto narrativo del resumen por grupo de un lote (Fase 5, Hito 5), generado por LLM (1:1 con
 * {@code lote}). Es lo único persistido del resumen — las estadísticas se recomputan on-demand —
 * porque regenerar el texto cuesta una llamada al modelo. {@code docenteId} replica el propietario
 * para el scoping por docente; {@code modelo} guarda qué modelo lo produjo (trazas/costo).
 */
@Entity
@Table(name = "resumen_grupo")
@Getter
@Setter
public class ResumenGrupo extends AbstractTenantEntity {

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(nullable = false, columnDefinition = "text")
    private String narrativa;

    @Column(length = 100)
    private String modelo;

    @Column(name = "generado_at", nullable = false)
    private Instant generadoAt;
}
