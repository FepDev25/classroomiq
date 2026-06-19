package com.classroomiq.backend.entrega.dto;

import java.util.List;
import java.util.UUID;

import com.classroomiq.backend.entrega.domain.RolArchivo;

/**
 * Contenido reconstruido de un archivo de la entrega: su identidad y las secciones de texto limpio
 * (re-extraídas en el momento, sin solapamiento de chunking ni costo de LLM/embeddings).
 *
 * @param archivoId      id del {@link com.classroomiq.backend.entrega.domain.ArchivoEntrega}
 * @param nombreOriginal nombre del archivo subido
 * @param rol            DOCUMENTO o CODIGO
 * @param secciones      secciones en orden de lectura
 */
public record ArchivoContenidoResponse(
        UUID archivoId,
        String nombreOriginal,
        RolArchivo rol,
        List<SeccionContenidoResponse> secciones) {
}
