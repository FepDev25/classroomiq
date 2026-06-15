package com.classroomiq.backend.provider.embeddings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Verificación fail-fast del proveedor de embeddings al arrancar.
 *
 * <p>Sólo se activa con {@code app.embeddings.verify-on-startup=true} (off por defecto, para no
 * atar el arranque local ni los tests a que Ollama esté levantado). Cuando está activa, embebe un
 * texto sonda y comprueba que la dimensión real coincide con la configurada — así un cambio de
 * modelo sin migrar el esquema {@code vector(N)} se detecta al iniciar y no al insertar.
 */
@Component
@ConditionalOnProperty(name = "app.embeddings.verify-on-startup", havingValue = "true")
class EmbeddingsStartupVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingsStartupVerifier.class);

    private final EmbeddingProvider provider;

    EmbeddingsStartupVerifier(EmbeddingProvider provider) {
        this.provider = provider;
    }

    @Override
    public void run(ApplicationArguments args) {
        float[] sonda = provider.embed("classroomiq embeddings healthcheck");
        if (sonda.length != provider.dimension()) {
            throw new IllegalStateException(
                    "Dimensión de embeddings inconsistente: modelo '%s' devolvió %d, configurado %d"
                            .formatted(provider.modelo(), sonda.length, provider.dimension()));
        }
        log.info("Embeddings verificados: modelo '{}', dimensión {}",
                provider.modelo(), provider.dimension());
    }
}
