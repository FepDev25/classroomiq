package com.classroomiq.backend.entrega.extraccion;

import java.nio.file.Path;
import java.util.List;

/**
 * Extrae texto estructurado de un archivo de entrega. Cada implementación maneja un tipo de
 * archivo (PDF, DOCX, ZIP de código). El orquestador selecciona el extractor por
 * {@link #soporta(String)} y agrega los resultados.
 */
public interface ExtractorArchivo {

    /** Indica si este extractor maneja el archivo según su nombre/extensión. */
    boolean soporta(String nombreArchivo);

    /**
     * Extrae los segmentos de texto del archivo.
     *
     * @param ruta         ruta absoluta del archivo en disco
     * @param nombreLogico nombre original (para la procedencia)
     * @throws ExtraccionException si el archivo está corrupto o no se puede procesar
     */
    List<SegmentoTexto> extraer(Path ruta, String nombreLogico);
}
