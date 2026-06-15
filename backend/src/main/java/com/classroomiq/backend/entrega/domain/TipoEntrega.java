package com.classroomiq.backend.entrega.domain;

/**
 * Tipo de entrega soportado desde el MVP.
 * DOCUMENTO: PDF o DOCX. CODIGO: ZIP con proyecto. MIXTA: documento + código juntos.
 */
public enum TipoEntrega {
    DOCUMENTO,
    CODIGO,
    MIXTA
}
