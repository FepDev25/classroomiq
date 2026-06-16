package com.classroomiq.backend.similitud.domain;

/**
 * Origen de una coincidencia de similitud entre dos fragmentos.
 *
 * <p>SEMANTICA: cercanía coseno de embeddings (mismas ideas, palabras distintas).
 * TEXTUAL: solapamiento de n-gramas de palabras (fragmentos copiados literalmente).
 */
public enum TipoSimilitud {
    SEMANTICA,
    TEXTUAL
}
