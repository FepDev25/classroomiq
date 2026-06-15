package com.classroomiq.backend.rubrica.domain;

import java.math.BigDecimal;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Nivel de desempeño de un criterio. El puntaje se expresa según {@link TipoPuntaje}:
 * RANGO usa puntajeMin/puntajeMax, FIJO usa puntajeValor, BANDA_PCT usa pctMin/pctMax.
 * Los campos no usados por el tipo quedan nulos.
 */
@Entity
@Table(name = "nivel_desempeno")
@Getter
@Setter
public class NivelDesempeno extends AbstractTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterio_id", nullable = false)
    private Criterio criterio;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 4000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_puntaje", nullable = false, length = 20)
    private TipoPuntaje tipoPuntaje;

    @Column(name = "puntaje_min", precision = 6, scale = 2)
    private BigDecimal puntajeMin;

    @Column(name = "puntaje_max", precision = 6, scale = 2)
    private BigDecimal puntajeMax;

    @Column(name = "puntaje_valor", precision = 6, scale = 2)
    private BigDecimal puntajeValor;

    @Column(name = "pct_min", precision = 5, scale = 2)
    private BigDecimal pctMin;

    @Column(name = "pct_max", precision = 5, scale = 2)
    private BigDecimal pctMax;

    @Column(nullable = false)
    private int orden;
}
