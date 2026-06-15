package com.classroomiq.backend.entrega.extraccion;

import java.util.UUID;

/**
 * Unidad de texto extraída de una entrega, con su procedencia. Es la salida común de todos los
 * extractores y la entrada del chunking (Hito 4); sus metadatos se trasladan a
 * {@link com.classroomiq.backend.entrega.domain.FragmentoEntrega} para el resaltado en revisión.
 *
 * @param archivoId   id del {@link com.classroomiq.backend.entrega.domain.ArchivoEntrega} de
 *                    origen; lo fija el orquestador (los extractores lo dejan en {@code null})
 * @param origen      nombre lógico de procedencia ("informe.pdf" o "src/main.py")
 * @param seccion     título de sección, página o celda (nullable)
 * @param lenguaje    lenguaje detectado para código (nullable)
 * @param lineaInicio primera línea (1-based) en el archivo de origen (nullable)
 * @param lineaFin    última línea en el archivo de origen (nullable)
 * @param contenido   texto del segmento
 */
public record SegmentoTexto(
        UUID archivoId,
        String origen,
        String seccion,
        String lenguaje,
        Integer lineaInicio,
        Integer lineaFin,
        String contenido) {

    /** Crea un segmento sin archivo asignado (lo completa el orquestador). */
    public static SegmentoTexto de(String origen, String seccion, String lenguaje,
            Integer lineaInicio, Integer lineaFin, String contenido) {
        return new SegmentoTexto(null, origen, seccion, lenguaje, lineaInicio, lineaFin, contenido);
    }

    /** Devuelve una copia con el id del archivo de origen fijado. */
    public SegmentoTexto conArchivo(UUID id) {
        return new SegmentoTexto(id, origen, seccion, lenguaje, lineaInicio, lineaFin, contenido);
    }
}
