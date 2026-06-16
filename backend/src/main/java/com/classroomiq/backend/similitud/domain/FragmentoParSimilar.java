package com.classroomiq.backend.similitud.domain;

import java.math.BigDecimal;
import java.util.UUID;

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
 * Fragmento concreto donde la similitud de un par es mayor, para la visualización lado a lado.
 *
 * <p>{@code fragmentoAId}/{@code fragmentoBId} referencian los chunks de origen (nulos si se
 * reindexa la entrega); {@code textoA}/{@code textoB} guardan la cita textual exacta. {@code tipo}
 * distingue si la coincidencia provino del análisis semántico o del textual.
 */
@Entity
@Table(name = "fragmento_par_similar")
@Getter
@Setter
public class FragmentoParSimilar extends AbstractTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "par_id", nullable = false)
    private ParSimilitud par;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSimilitud tipo;

    @Column(name = "fragmento_a_id")
    private UUID fragmentoAId;

    @Column(name = "fragmento_b_id")
    private UUID fragmentoBId;

    @Column(name = "texto_a", nullable = false, columnDefinition = "text")
    private String textoA;

    @Column(name = "texto_b", nullable = false, columnDefinition = "text")
    private String textoB;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal similitud;

    @Column(nullable = false)
    private int orden;
}
