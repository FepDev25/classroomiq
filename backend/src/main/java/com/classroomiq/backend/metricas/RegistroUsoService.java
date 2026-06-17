package com.classroomiq.backend.metricas;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.classroomiq.backend.metricas.domain.OperacionLlm;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.ModeloTier;

/**
 * Punto de entrada para registrar el uso del LLM (Fase 6, Hito 1) desde los call-sites del motor de
 * evaluación y de la narrativa de grupo. Es <strong>fail-open</strong>: si el registro de la métrica
 * falla por cualquier razón, se loguea y se sigue — una métrica de uso nunca debe tumbar la
 * evaluación, que es el trabajo real del docente.
 *
 * <p>Delega la persistencia en {@link RegistroUsoWriter}, que corre en una transacción propia para
 * que el costo sobreviva aunque el trabajo que lo originó revierta. La captura de la excepción vive
 * aquí, fuera del proxy transaccional del writer.
 */
@Service
public class RegistroUsoService {

    private static final Logger log = LoggerFactory.getLogger(RegistroUsoService.class);

    private final RegistroUsoWriter writer;

    public RegistroUsoService(RegistroUsoWriter writer) {
        this.writer = writer;
    }

    /** Registra el consumo de una evaluación por criterio (tier potente). */
    public void registrarEvaluacion(UUID docenteId, UUID entregaId, ModeloTier tier, LlmResultado resultado) {
        registrar(OperacionLlm.EVALUACION, tier, docenteId, entregaId, null, resultado);
    }

    /** Registra el consumo de la narrativa de un resumen por grupo (tier económico). */
    public void registrarNarrativa(UUID docenteId, UUID loteId, ModeloTier tier, LlmResultado resultado) {
        registrar(OperacionLlm.NARRATIVA, tier, docenteId, null, loteId, resultado);
    }

    private void registrar(OperacionLlm operacion, ModeloTier tier, UUID docenteId,
            UUID entregaId, UUID loteId, LlmResultado resultado) {
        if (resultado == null || resultado.uso() == null) {
            return; // el proveedor no informó tokens: nada que registrar
        }
        try {
            writer.persistir(operacion, tier, docenteId, entregaId, loteId, resultado);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el uso del LLM (op={}, docente={}): {}",
                    operacion, docenteId, e.toString());
        }
    }
}
