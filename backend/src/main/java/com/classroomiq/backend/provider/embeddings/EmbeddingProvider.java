package com.classroomiq.backend.provider.embeddings;

import java.util.List;

/**
 * Proveedor de embeddings intercambiable (Fase 3).
 *
 * <p>Abstrae el modelo concreto detrás de una interfaz para poder alternar entre proveedores
 * <em>cloud</em> y <em>local self-hosted</em> por configuración ({@code app.embeddings.provider}).
 * La primera implementación es {@link OllamaEmbeddingProvider} (bge-m3 vía Ollama local).
 *
 * <p>Los vectores devueltos están <strong>normalizados (norma L2 = 1)</strong>, de modo que el
 * producto punto equivale a la similitud coseno usada por pgvector en las fases siguientes.
 */
public interface EmbeddingProvider {

    /**
     * Genera el embedding de cada texto, en el mismo orden de entrada.
     *
     * @param textos textos a vectorizar (no nulo; puede ser vacío)
     * @return un vector normalizado por texto, cada uno de longitud {@link #dimension()}
     * @throws EmbeddingException si el proveedor falla o devuelve una dimensión inesperada
     */
    List<float[]> embed(List<String> textos);

    /** Conveniencia para un único texto. */
    default float[] embed(String texto) {
        return embed(List.of(texto)).get(0);
    }

    /** Dimensión esperada de los vectores (debe coincidir con la columna {@code vector(N)}). */
    int dimension();

    /** Identificador del modelo en uso (para trazas y métricas). */
    String modelo();
}
