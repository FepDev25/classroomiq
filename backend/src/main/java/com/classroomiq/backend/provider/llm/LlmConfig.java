package com.classroomiq.backend.provider.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

/**
 * Cableado del módulo de LLM: habilita {@link LlmProperties} y construye el {@link AnthropicClient}
 * con la API key desde configuración. El SDK reintenta 429/5xx con backoff por defecto.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
class LlmConfig {

    @Bean
    AnthropicClient anthropicClient(LlmProperties properties) {
        return AnthropicOkHttpClient.builder()
                .apiKey(properties.anthropic().apiKey())
                .build();
    }
}
