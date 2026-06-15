package com.classroomiq.backend.provider.llm;

/**
 * Proveedor de LLM intercambiable (Fase 4), análogo a {@code EmbeddingProvider} de Fase 3.
 *
 * <p>Abstrae el modelo concreto detrás de una interfaz para alternar entre proveedores
 * <em>cloud</em> (Anthropic) y, a futuro, <em>local self-hosted</em> por configuración
 * ({@code app.llm.provider}). La primera implementación es {@link AnthropicLlmProvider}.
 *
 * <p>Expone una estrategia de dos niveles ({@link ModeloTier}): un modelo <strong>potente</strong>
 * para el análisis de evaluación por criterio y uno <strong>económico</strong> para tareas
 * simples. El motor de evaluación (Fase 4) usa el potente; el económico queda disponible para
 * clasificación/extracción de fases posteriores.
 */
public interface LlmProvider {

    /**
     * Genera una respuesta para la solicitud dada.
     *
     * @param solicitud tier de modelo + prompts (no nulo)
     * @return texto generado + modelo + motivo de fin + tokens consumidos
     * @throws LlmException si el proveedor falla o rechaza la solicitud
     */
    LlmResultado generar(LlmSolicitud solicitud);

    /** Identificador del modelo configurado para el tier indicado (para trazas y métricas). */
    String modelo(ModeloTier tier);
}
