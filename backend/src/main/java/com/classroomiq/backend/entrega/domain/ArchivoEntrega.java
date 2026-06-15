package com.classroomiq.backend.entrega.domain;

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
 * Archivo físico que compone una entrega. El binario vive en el sistema de archivos del servidor
 * (ver Hito 2); aquí se guardan la ruta relativa, metadatos y el hash para integridad/dedupe.
 */
@Entity
@Table(name = "archivo_entrega")
@Getter
@Setter
public class ArchivoEntrega extends AbstractTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    @Column(name = "nombre_original", nullable = false, length = 512)
    private String nombreOriginal;

    @Column(name = "ruta_relativa", nullable = false, length = 1024)
    private String rutaRelativa;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolArchivo rol;

    @Column(nullable = false)
    private int orden;
}
