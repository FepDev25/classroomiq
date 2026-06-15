package com.classroomiq.backend.materia.domain;

import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Materia de un docente. Agrupa rúbricas y entregas. Puede archivarse al cierre del ciclo.
 * {@code docenteId} es el propietario: el aislamiento por docente se aplica sobre esta columna.
 */
@Entity
@Table(name = "materia")
@Getter
@Setter
public class Materia extends AbstractTenantEntity {

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "periodo_academico", length = 100)
    private String periodoAcademico;

    @Column(length = 4000)
    private String descripcion;

    @Column(nullable = false)
    private boolean archivada = false;
}
