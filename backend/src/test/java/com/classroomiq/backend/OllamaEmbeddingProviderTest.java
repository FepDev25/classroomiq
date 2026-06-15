package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.classroomiq.backend.provider.embeddings.EmbeddingException;
import com.classroomiq.backend.provider.embeddings.EmbeddingsProperties;
import com.classroomiq.backend.provider.embeddings.OllamaEmbeddingProvider;

/**
 * Tests unitarios del {@link OllamaEmbeddingProvider} sin Ollama real: la respuesta HTTP se
 * simula con {@link MockRestServiceServer}. Se valida parseo, normalización L2, orden, batching
 * y el fail-fast cuando la dimensión no coincide.
 */
class OllamaEmbeddingProviderTest {

    private static final int DIMENSION = 3;

    private record Fixture(OllamaEmbeddingProvider provider, MockRestServiceServer server) {
    }

    private Fixture nuevo(int batchSize) {
        EmbeddingsProperties props = new EmbeddingsProperties("ollama", DIMENSION, false,
                new EmbeddingsProperties.Ollama(
                        "http://localhost:11434", "bge-m3", Duration.ofSeconds(5), batchSize));
        RestClient.Builder builder = RestClient.builder().baseUrl(props.ollama().baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new OllamaEmbeddingProvider(builder.build(), props), server);
    }

    @Test
    void embedDevuelveVectoresNormalizadosEnOrden() {
        Fixture f = nuevo(16);
        f.server().expect(requestTo("http://localhost:11434/api/embed"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"embeddings\":[[3.0,4.0,0.0],[0.0,0.0,2.0]]}",
                        MediaType.APPLICATION_JSON));

        List<float[]> vectores = f.provider().embed(List.of("hola", "mundo"));

        f.server().verify();
        assertThat(vectores).hasSize(2);
        // [3,4,0] normalizado -> [0.6, 0.8, 0]
        assertThat(vectores.get(0)).usingComparatorWithPrecision(1e-6f).containsExactly(0.6f, 0.8f, 0.0f);
        // [0,0,2] normalizado -> [0, 0, 1]
        assertThat(vectores.get(1)).usingComparatorWithPrecision(1e-6f).containsExactly(0.0f, 0.0f, 1.0f);
    }

    @Test
    void dimensionInesperadaLanzaExcepcion() {
        Fixture f = nuevo(16);
        f.server().expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess("{\"embeddings\":[[1.0,2.0]]}", MediaType.APPLICATION_JSON));

        assertThatExceptionOfType(EmbeddingException.class)
                .isThrownBy(() -> f.provider().embed(List.of("texto")))
                .withMessageContaining("Dimensión inesperada");
    }

    @Test
    void entradaVaciaNoContactaAlProveedor() {
        Fixture f = nuevo(16);
        // Sin expectativas declaradas: cualquier llamada HTTP haría fallar verify().
        assertThat(f.provider().embed(List.of())).isEmpty();
        f.server().verify();
    }

    @Test
    void respetaElTamanoDeLoteEnLlamadasSeparadas() {
        Fixture f = nuevo(1);
        f.server().expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess("{\"embeddings\":[[1.0,0.0,0.0]]}", MediaType.APPLICATION_JSON));
        f.server().expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess("{\"embeddings\":[[0.0,1.0,0.0]]}", MediaType.APPLICATION_JSON));

        List<float[]> vectores = f.provider().embed(List.of("a", "b"));

        f.server().verify();
        assertThat(vectores).hasSize(2);
        assertThat(vectores.get(0)).usingComparatorWithPrecision(1e-6f).containsExactly(1.0f, 0.0f, 0.0f);
        assertThat(vectores.get(1)).usingComparatorWithPrecision(1e-6f).containsExactly(0.0f, 1.0f, 0.0f);
    }
}
