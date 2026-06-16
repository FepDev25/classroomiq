package com.classroomiq.backend.similitud.domain;

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
 * Similitud entre un par no ordenado de entregas del lote.
 *
 * <p>Por convención {@code entregaAId < entregaBId} para no duplicar el par. {@code similitudSemantica}
 * es el coseno de los centroides de embeddings de cada entrega; {@code similitudTextual} es el
 * solapamiento de n-gramas (nula si alguna entrega no tiene prosa comparable). {@code superaUmbral}
 * marca el par para revisión manual cuando la similitud semántica iguala o supera el umbral del reporte.
 */
@Entity
@Table(name = "par_similitud")
@Getter
@Setter
public class ParSimilitud extends AbstractTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false)
    private ReporteSimilitud reporte;

    @Column(name = "entrega_a_id", nullable = false)
    private UUID entregaAId;

    @Column(name = "entrega_b_id", nullable = false)
    private UUID entregaBId;

    @Column(name = "similitud_semantica", nullable = false, precision = 5, scale = 4)
    private BigDecimal similitudSemantica;

    @Column(name = "similitud_textual", precision = 5, scale = 4)
    private BigDecimal similitudTextual;

    @Column(name = "supera_umbral", nullable = false)
    private boolean superaUmbral = false;

    @OneToMany(mappedBy = "par", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<FragmentoParSimilar> fragmentos = new ArrayList<>();

    public void addFragmento(FragmentoParSimilar fragmento) {
        fragmento.setPar(this);
        fragmentos.add(fragmento);
    }
}
