package com.classroomiq.backend.entrega.domain;

/**
 * Estado de procesamiento de una entrega individual.
 * PENDIENTE: en cola. PROCESANDO: extrayendo texto y generando embeddings.
 * EVALUANDO: el LLM genera el borrador (Fase 4). LISTO: borrador disponible.
 * ERROR: fallo en el procesamiento (ver {@code mensajeError}).
 */
public enum EstadoEntrega {
    PENDIENTE,
    PROCESANDO,
    EVALUANDO,
    LISTO,
    ERROR
}
