package com.classroomiq.backend.provider.embeddings;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cableado del módulo de embeddings: habilita {@link EmbeddingsProperties} y construye el
 * {@link RestClient} dedicado a Ollama (URL base y timeouts desde configuración).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EmbeddingsProperties.class)
class EmbeddingsConfig {

    @Bean
    RestClient ollamaRestClient(RestClient.Builder builder, EmbeddingsProperties properties) {
        var timeout = properties.ollama().timeout();
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(timeout)
                .withReadTimeout(timeout);
        return builder
                .baseUrl(properties.ollama().baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
