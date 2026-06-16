package com.classroomiq.backend.similitud.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Reporte de similitud de un lote (1:1 con {@code lote}). Raíz del agregado
 * reporte → pares → fragmentos similares.
 *
 * <p>Se calcula una vez sobre las entregas LISTO del lote y se persiste: el cálculo de pares es
 * O(n²) y el resultado es estable. Su existencia indica que el lote ya fue analizado. Reanalizar
 * borra el reporte previo en cascada. {@code docenteId} replica el propietario para el scoping por
 * docente; {@code umbral} guarda el valor de "similitud alta" usado al generarlo (default 0.75).
 */
@Entity
@Table(name = "reporte_similitud")
@Getter
@Setter
public class ReporteSimilitud extends AbstractTenantEntity {

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(name = "materia_id", nullable = false)
    private UUID materiaId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal umbral;

    @Column(name = "generado_at", nullable = false)
    private Instant generadoAt;

    @OneToMany(mappedBy = "reporte", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParSimilitud> pares = new ArrayList<>();

    public void addPar(ParSimilitud par) {
        par.setReporte(this);
        pares.add(par);
    }
}
