package com.classroomiq.backend.provider.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Verificación opt-in del proveedor de LLM al arrancar.
 *
 * <p>Sólo se activa con {@code app.llm.verify-on-startup=true} (off por defecto, para no atar el
 * arranque ni los tests a la disponibilidad del proveedor — ni gastar tokens en cada boot). Cuando
 * está activa, hace una llamada sonda mínima con el tier económico y comprueba que devuelve texto;
 * así un problema de API key o de conectividad se detecta al iniciar y no en la primera evaluación.
 */
@Component
@ConditionalOnProperty(name = "app.llm.verify-on-startup", havingValue = "true")
class LlmStartupVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LlmStartupVerifier.class);

    private final LlmProvider provider;

    LlmStartupVerifier(LlmProvider provider) {
        this.provider = provider;
    }

    @Override
    public void run(ApplicationArguments args) {
        LlmResultado sonda = provider.generar(new LlmSolicitud(
                ModeloTier.ECONOMICO, null, "Responde únicamente con la palabra: OK"));
        log.info("LLM verificado: modelo '{}', {} tokens de salida, stop '{}'",
                sonda.modelo(), sonda.uso().outputTokens(), sonda.stopReason());
    }
}
