package com.classroomiq.backend.entrega.domain;

/**
 * Estado de un lote de entregas.
 * ABIERTO: se pueden subir entregas. PROCESANDO: hay entregas en procesamiento.
 * LISTO: todas las entregas terminaron (listas o en error).
 */
public enum EstadoLote {
    ABIERTO,
    PROCESANDO,
    LISTO
}
