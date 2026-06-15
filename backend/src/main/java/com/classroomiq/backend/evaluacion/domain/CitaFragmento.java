package com.classroomiq.backend.evaluacion.domain;

import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Cita textual del trabajo que el LLM usa como evidencia al justificar un criterio. Alimenta el
 * resaltado del fragmento en la pantalla de revisión (panel izquierdo).
 *
 * <p>{@code fragmentoId} apunta al chunk de origen en {@code fragmento_entrega} (puede quedar nulo
 * si la entrega se reindexa); {@code textoCitado} guarda el fragmento exacto citado, que puede ser
 * una subcadena del chunk.
 */
@Entity
@Table(name = "cita_fragmento")
@Getter
@Setter
public class CitaFragmento extends AbstractTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_criterio_id", nullable = false)
    private EvaluacionCriterio evaluacionCriterio;

    /** Chunk de origen en {@code fragmento_entrega}; nulo si el fragmento ya no existe. */
    @Column(name = "fragmento_id")
    private UUID fragmentoId;

    @Column(name = "texto_citado", nullable = false, columnDefinition = "text")
    private String textoCitado;

    @Column(nullable = false)
    private int orden;
}
