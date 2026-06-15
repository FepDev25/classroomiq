package com.classroomiq.backend.rubrica.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
 * Criterio de una rúbrica. La {@code descripcion} es el contexto principal que recibe el LLM.
 * {@code evaluablePorContenido=false} marca criterios que requieren juicio del docente
 * (demo, exposición, estética) y que el motor no debe puntuar.
 */
@Entity
@Table(name = "criterio")
@Getter
@Setter
public class Criterio extends AbstractTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubrica_id", nullable = false)
    private Rubrica rubrica;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 4000)
    private String descripcion;

    @Column(name = "puntaje_maximo", nullable = false, precision = 6, scale = 2)
    private BigDecimal puntajeMaximo;

    @Column(name = "evaluable_por_contenido", nullable = false)
    private boolean evaluablePorContenido = true;

    @Column(nullable = false)
    private int orden;

    @OneToMany(mappedBy = "criterio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<NivelDesempeno> niveles = new ArrayList<>();

    public void addNivel(NivelDesempeno nivel) {
        nivel.setCriterio(this);
        niveles.add(nivel);
    }

    public void removeNivel(NivelDesempeno nivel) {
        niveles.remove(nivel);
        nivel.setCriterio(null);
    }
}
