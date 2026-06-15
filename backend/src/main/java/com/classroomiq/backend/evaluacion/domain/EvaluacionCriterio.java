package com.classroomiq.backend.evaluacion.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Evaluación de un criterio de la rúbrica dentro del borrador de una entrega.
 *
 * <p>Lo que propone el motor: {@code nivelSugeridoId}, {@code puntajeSugerido},
 * {@code justificacion}, {@code advertencia} (cuando el contenido es insuficiente para evaluar con
 * confianza). Lo que ajusta el docente en la revisión: {@code nivelFinalId}, {@code puntajeFinal},
 * {@code justificacionEditada}, {@code revisadoManual}.
 *
 * <p>{@code criterioId} referencia el criterio de la rúbrica (no es relación JPA, como el resto del
 * dominio). {@code evaluable=false} marca los criterios que el motor no puntúa porque requieren
 * juicio del docente ({@code evaluablePorContenido=false}): se crean en blanco para que los llene.
 */
@Entity
@Table(name = "evaluacion_criterio")
@Getter
@Setter
public class EvaluacionCriterio extends AbstractTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    private Evaluacion evaluacion;

    @Column(name = "criterio_id", nullable = false)
    private UUID criterioId;

    @Column(nullable = false)
    private boolean evaluable = true;

    @Column(name = "nivel_sugerido_id")
    private UUID nivelSugeridoId;

    @Column(name = "puntaje_sugerido", precision = 6, scale = 2)
    private BigDecimal puntajeSugerido;

    @Column(columnDefinition = "text")
    private String justificacion;

    /** Aviso del motor cuando el trabajo no tiene contenido suficiente para evaluar el criterio. */
    @Column(columnDefinition = "text")
    private String advertencia;

    @Column(name = "nivel_final_id")
    private UUID nivelFinalId;

    @Column(name = "puntaje_final", precision = 6, scale = 2)
    private BigDecimal puntajeFinal;

    @Column(name = "justificacion_editada", columnDefinition = "text")
    private String justificacionEditada;

    @Column(name = "revisado_manual", nullable = false)
    private boolean revisadoManual = false;

    @Column(nullable = false)
    private int orden;

    @OneToMany(mappedBy = "evaluacionCriterio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<CitaFragmento> citas = new ArrayList<>();

    public void addCita(CitaFragmento cita) {
        cita.setEvaluacionCriterio(this);
        citas.add(cita);
    }

    public void removeCita(CitaFragmento cita) {
        citas.remove(cita);
        cita.setEvaluacionCriterio(null);
    }
}
