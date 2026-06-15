package com.classroomiq.backend.rubrica.domain;

import java.math.BigDecimal;
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
 * Rúbrica de evaluación de una materia. Raíz del agregado rúbrica → criterios → niveles.
 * Modelo de puntaje: puntos absolutos por criterio; el total se calcula según {@link ModoTotal}.
 */
@Entity
@Table(name = "rubrica")
@Getter
@Setter
public class Rubrica extends AbstractTenantEntity {

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(name = "materia_id", nullable = false)
    private UUID materiaId;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 4000)
    private String descripcion;

    @Column(name = "puntaje_total", nullable = false, precision = 6, scale = 2)
    private BigDecimal puntajeTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_total", nullable = false, length = 20)
    private ModoTotal modoTotal;

    @OneToMany(mappedBy = "rubrica", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Criterio> criterios = new ArrayList<>();

    public void addCriterio(Criterio criterio) {
        criterio.setRubrica(this);
        criterios.add(criterio);
    }

    public void removeCriterio(Criterio criterio) {
        criterios.remove(criterio);
        criterio.setRubrica(null);
    }
}
