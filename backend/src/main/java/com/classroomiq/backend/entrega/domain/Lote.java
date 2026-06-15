package com.classroomiq.backend.entrega.domain;

import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Lote de entregas asociado a una materia y una rúbrica (ej. "Proyecto Final — Grupo A").
 * Agrupa las entregas de los estudiantes que se procesan y evalúan en conjunto.
 * {@code docenteId} es el propietario: el aislamiento por docente se aplica sobre esta columna.
 * Las entregas referencian el lote por id (no es una colección en cascada): se procesan de
 * forma independiente en background.
 */
@Entity
@Table(name = "lote")
@Getter
@Setter
public class Lote extends AbstractTenantEntity {

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(name = "materia_id", nullable = false)
    private UUID materiaId;

    @Column(name = "rubrica_id", nullable = false)
    private UUID rubricaId;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLote estado = EstadoLote.ABIERTO;
}
