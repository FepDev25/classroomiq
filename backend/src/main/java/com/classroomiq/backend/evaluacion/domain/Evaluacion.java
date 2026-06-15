package com.classroomiq.backend.evaluacion.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Borrador de evaluación de una entrega (1:1 con {@code entrega}). Raíz del agregado
 * evaluación → criterios → citas.
 *
 * <p>Su existencia distingue una entrega <em>indexada</em> (Fase 3, sin borrador) de una
 * <em>evaluada</em>: el motor de evaluación (paso explícito, separado del indexado) crea esta fila.
 * {@code docenteId} replica el propietario para el scoping por docente a nivel de repositorio.
 *
 * <p>Los puntajes totales (sugerido y final) se calculan según el {@code ModoTotal} de la rúbrica
 * a partir de los criterios; se almacenan para los reportes agregados por grupo (Fase 5).
 */
@Entity
@Table(name = "evaluacion")
@Getter
@Setter
public class Evaluacion extends AbstractTenantEntity {

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(name = "entrega_id", nullable = false)
    private UUID entregaId;

    @Column(name = "rubrica_id", nullable = false)
    private UUID rubricaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvaluacion estado = EstadoEvaluacion.BORRADOR;

    @Column(name = "puntaje_total_sugerido", precision = 7, scale = 2)
    private BigDecimal puntajeTotalSugerido;

    @Column(name = "puntaje_total_final", precision = 7, scale = 2)
    private BigDecimal puntajeTotalFinal;

    @Column(name = "comentario_general", length = 4000)
    private String comentarioGeneral;

    @Column(name = "aprobada_at")
    private Instant aprobadaAt;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<EvaluacionCriterio> criterios = new ArrayList<>();

    public void addCriterio(EvaluacionCriterio criterio) {
        criterio.setEvaluacion(this);
        criterios.add(criterio);
    }

    public void removeCriterio(EvaluacionCriterio criterio) {
        criterios.remove(criterio);
        criterio.setEvaluacion(null);
    }
}
