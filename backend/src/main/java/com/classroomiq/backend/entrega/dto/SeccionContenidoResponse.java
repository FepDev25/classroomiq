package com.classroomiq.backend.entrega.dto;

/**
 * Una sección de texto del documento reconstruido (página de PDF, título de DOCX, archivo o celda
 * de código). Es la unidad que el panel de revisión renderiza y sobre la que resalta las citas.
 *
 * @param titulo      título de sección, página o celda (nullable)
 * @param lenguaje    lenguaje detectado si es código (nullable)
 * @param lineaInicio primera línea 1-based en el archivo de origen (nullable)
 * @param lineaFin    última línea en el archivo de origen (nullable)
 * @param texto       texto limpio de la sección
 */
public record SeccionContenidoResponse(
        String titulo,
        String lenguaje,
        Integer lineaInicio,
        Integer lineaFin,
        String texto) {
}
