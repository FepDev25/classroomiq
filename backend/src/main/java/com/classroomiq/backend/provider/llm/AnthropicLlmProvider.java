package com.classroomiq.backend.provider.llm;

import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ThinkingConfigAdaptive;

/**
 * Implementación de {@link LlmProvider} sobre la API de Anthropic vía el SDK oficial.
 *
 * <p>Estrategia de dos niveles: el tier <strong>potente</strong> ({@code claude-sonnet-4-6}) corre
 * con <em>thinking adaptativo</em> y el parámetro {@code effort} para maximizar la calidad del
 * borrador; el tier <strong>económico</strong> ({@code claude-haiku-4-5}) corre sin esos
 * parámetros — Haiku no soporta {@code effort} y devolvería 400.
 *
 * <p>Es el proveedor por defecto ({@code matchIfMissing = true}); cuando se añada un proveedor
 * local self-hosted se seleccionará con {@code app.llm.provider}.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "anthropic", matchIfMissing = true)
public class AnthropicLlmProvider implements LlmProvider {

    private final AnthropicClient client;
    private final LlmProperties properties;

    public AnthropicLlmProvider(AnthropicClient client, LlmProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public LlmResultado generar(LlmSolicitud solicitud) {
        String modelo = modelo(solicitud.tier());

        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(properties.maxTokens())
                .addUserMessage(solicitud.prompt());

        if (solicitud.system() != null && !solicitud.system().isBlank()) {
            builder.system(solicitud.system());
        }
        // Thinking adaptativo + effort solo en el tier potente. El SDK rechaza effort en Haiku.
        if (solicitud.tier() == ModeloTier.POTENTE) {
            builder.thinking(ThinkingConfigAdaptive.builder().build());
            builder.outputConfig(OutputConfig.builder().effort(effort()).build());
        }

        Message respuesta;
        try {
            respuesta = client.messages().create(builder.build());
        } catch (RuntimeException e) {
            throw new LlmException("Fallo al invocar Anthropic (modelo '%s')".formatted(modelo), e);
        }

        String texto = respuesta.content().stream()
                .flatMap(bloque -> bloque.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());
        String stopReason = respuesta.stopReason().map(Object::toString).orElse(null);
        UsoTokens uso = new UsoTokens(respuesta.usage().inputTokens(), respuesta.usage().outputTokens());

        return new LlmResultado(texto, modelo, stopReason, uso);
    }

    @Override
    public String modelo(ModeloTier tier) {
        return tier == ModeloTier.POTENTE ? properties.modeloPotente() : properties.modeloEconomico();
    }

    private OutputConfig.Effort effort() {
        return switch (properties.effort().trim().toLowerCase(Locale.ROOT)) {
            case "low" -> OutputConfig.Effort.LOW;
            case "medium" -> OutputConfig.Effort.MEDIUM;
            case "high" -> OutputConfig.Effort.HIGH;
            case "max" -> OutputConfig.Effort.MAX;
            default -> throw new LlmException(
                    "Valor de app.llm.effort inválido: '%s' (use low|medium|high|max)"
                            .formatted(properties.effort()));
        };
    }
}
