package com.classroomiq.backend.entrega.dto;

import java.util.List;
import java.util.UUID;

/**
 * Texto completo de una entrega, reconstruido bajo demanda para el panel de revisión. Permite mostrar
 * el documento íntegro con los fragmentos citados por el motor resaltados en su contexto, en vez de
 * la lista suelta de citas. Se obtiene re-extrayendo los archivos en disco (reusa el pipeline de
 * extracción de la Fase 3): no consume tokens de LLM ni embeddings.
 *
 * @param entregaId id de la entrega
 * @param archivos  archivos de la entrega con su contenido por secciones
 */
public record ContenidoEntregaResponse(
        UUID entregaId,
        List<ArchivoContenidoResponse> archivos) {
}
