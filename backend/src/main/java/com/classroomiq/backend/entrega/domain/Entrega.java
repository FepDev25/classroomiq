package com.classroomiq.backend.entrega.domain;

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
 * Entrega de un estudiante dentro de un lote. Raíz del agregado entrega → archivos.
 * El {@code identificadorEstudiante} puede ser un alias o número de grupo (no requiere PII).
 * {@code materiaId}/{@code loteId}/{@code docenteId} se replican para el scoping de similitud.
 * Los fragmentos vectorizados NO son una colección en cascada (pueden ser muchos): se gestionan
 * aparte por el pipeline de procesamiento.
 */
@Entity
@Table(name = "entrega")
@Getter
@Setter
public class Entrega extends AbstractTenantEntity {

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(name = "materia_id", nullable = false)
    private UUID materiaId;

    @Column(name = "identificador_estudiante", nullable = false)
    private String identificadorEstudiante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEntrega tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEntrega estado = EstadoEntrega.PENDIENTE;

    @Column(name = "mensaje_error", length = 4000)
    private String mensajeError;

    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ArchivoEntrega> archivos = new ArrayList<>();

    public void addArchivo(ArchivoEntrega archivo) {
        archivo.setEntrega(this);
        archivos.add(archivo);
    }

    public void removeArchivo(ArchivoEntrega archivo) {
        archivos.remove(archivo);
        archivo.setEntrega(null);
    }
}
