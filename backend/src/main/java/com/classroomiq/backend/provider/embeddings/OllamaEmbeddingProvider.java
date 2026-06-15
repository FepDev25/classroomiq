package com.classroomiq.backend.provider.embeddings;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementación de {@link EmbeddingProvider} sobre un servidor Ollama local.
 *
 * <p>Usa el endpoint batch {@code POST /api/embed} (admite varios textos por llamada). Divide la
 * entrada en lotes de {@code app.embeddings.ollama.batch-size}, valida que cada vector tenga la
 * dimensión configurada y lo normaliza (L2) para que el producto punto sea la similitud coseno.
 *
 * <p>Es el proveedor por defecto ({@code matchIfMissing = true}); cuando se añadan proveedores
 * cloud se seleccionarán con {@code app.embeddings.provider}.
 */
@Component
@ConditionalOnProperty(name = "app.embeddings.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final RestClient client;
    private final EmbeddingsProperties properties;

    public OllamaEmbeddingProvider(RestClient ollamaRestClient, EmbeddingsProperties properties) {
        this.client = ollamaRestClient;
        this.properties = properties;
    }

    @Override
    public List<float[]> embed(List<String> textos) {
        if (textos.isEmpty()) {
            return List.of();
        }
        int batchSize = properties.ollama().batchSize();
        List<float[]> resultado = new ArrayList<>(textos.size());
        for (int desde = 0; desde < textos.size(); desde += batchSize) {
            int hasta = Math.min(desde + batchSize, textos.size());
            resultado.addAll(embedLote(textos.subList(desde, hasta)));
        }
        return resultado;
    }

    private List<float[]> embedLote(List<String> lote) {
        OllamaEmbedResponse respuesta;
        try {
            respuesta = client.post()
                    .uri("/api/embed")
                    .body(new OllamaEmbedRequest(properties.ollama().model(), lote))
                    .retrieve()
                    .body(OllamaEmbedResponse.class);
        } catch (RestClientException e) {
            throw new EmbeddingException(
                    "Fallo al contactar Ollama en " + properties.ollama().baseUrl(), e);
        }
        if (respuesta == null || respuesta.embeddings() == null) {
            throw new EmbeddingException("Ollama devolvió una respuesta vacía de embeddings");
        }
        if (respuesta.embeddings().size() != lote.size()) {
            throw new EmbeddingException("Ollama devolvió %d embeddings para %d textos"
                    .formatted(respuesta.embeddings().size(), lote.size()));
        }
        List<float[]> vectores = new ArrayList<>(lote.size());
        for (List<Float> embedding : respuesta.embeddings()) {
            vectores.add(normalizar(embedding));
        }
        return vectores;
    }

    private float[] normalizar(List<Float> embedding) {
        if (embedding.size() != properties.dimension()) {
            throw new EmbeddingException("Dimensión inesperada del embedding: esperaba %d, llegó %d"
                    .formatted(properties.dimension(), embedding.size()));
        }
        double sumaCuadrados = 0.0;
        for (float v : embedding) {
            sumaCuadrados += (double) v * v;
        }
        double norma = Math.sqrt(sumaCuadrados);
        float[] vector = new float[embedding.size()];
        if (norma == 0.0) {
            // Vector cero: no se puede normalizar; se devuelve tal cual para no propagar NaN.
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i);
            }
            return vector;
        }
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = (float) (embedding.get(i) / norma);
        }
        return vector;
    }

    @Override
    public int dimension() {
        return properties.dimension();
    }

    @Override
    public String modelo() {
        return properties.ollama().model();
    }

    /** Cuerpo de la petición a {@code /api/embed}. */
    record OllamaEmbedRequest(String model, List<String> input) {
    }

    /** Respuesta de {@code /api/embed}: un embedding por texto, en orden. */
    record OllamaEmbedResponse(List<List<Float>> embeddings) {
    }
}
