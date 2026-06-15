package com.classroomiq.backend.entrega.domain;

import java.util.UUID;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Fragmento (chunk) de texto de una entrega con su embedding, almacenado en pgvector.
 *
 * <p>El {@code embedding} se mapea a la columna {@code vector(1024)} vía el módulo
 * {@code hibernate-vector} ({@link SqlTypes#VECTOR} + {@link Array}). Está normalizado L2, por lo
 * que la similitud coseno se calcula con el operador {@code <=>} de pgvector (Fase 5).
 *
 * <p>Replica {@code docenteId}/{@code materiaId}/{@code loteId} como metadatos para que la
 * búsqueda de similitud quede acotada al mismo docente, materia y lote. La procedencia
 * ({@code archivoId}, {@code seccion}, líneas) permite el resaltado en la pantalla de revisión.
 */
@Entity
@Table(name = "fragmento_entrega")
@Getter
@Setter
public class FragmentoEntrega extends AbstractTenantEntity {

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(name = "materia_id", nullable = false)
    private UUID materiaId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "entrega_id", nullable = false)
    private UUID entregaId;

    /** Archivo de origen del fragmento; nulo si proviene de texto consolidado. */
    @Column(name = "archivo_id")
    private UUID archivoId;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    /** Sección o archivo lógico de procedencia (para contexto y resaltado). */
    @Column(length = 512)
    private String seccion;

    @Column(name = "linea_inicio")
    private Integer lineaInicio;

    @Column(name = "linea_fin")
    private Integer lineaFin;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1024)
    @Column(nullable = false)
    private float[] embedding;
}
