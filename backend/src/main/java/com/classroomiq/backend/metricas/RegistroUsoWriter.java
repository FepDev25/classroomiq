package com.classroomiq.backend.metricas;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.metricas.domain.OperacionLlm;
import com.classroomiq.backend.metricas.domain.RegistroUsoLlm;
import com.classroomiq.backend.metricas.repository.RegistroUsoLlmRepository;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.ModeloTier;

/**
 * Escribe una fila en el libro mayor de uso del LLM (Fase 6, Hito 1) en una transacción propia
 * ({@link Propagation#REQUIRES_NEW}): los tokens ya se gastaron, así que el costo debe quedar
 * registrado aunque la evaluación o la narrativa que lo originó falle y revierta su propia tx.
 *
 * <p>Bean separado del fail-open ({@link RegistroUsoService}) a propósito: el try/catch que tolera
 * el fallo debe estar FUERA del proxy transaccional para que el rollback de esta tx interna se
 * complete limpio sin propagar {@code UnexpectedRollbackException} al llamador. El {@code tenant_id}
 * lo estampa {@code @TenantId} desde el {@code TenantContext} del hilo actual (la tx nueva corre en
 * el mismo hilo, así que el tenant sigue fijado por el worker o el filtro de request).
 */
@Component
public class RegistroUsoWriter {

    private final RegistroUsoLlmRepository registros;

    public RegistroUsoWriter(RegistroUsoLlmRepository registros) {
        this.registros = registros;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistir(OperacionLlm operacion, ModeloTier tier, UUID docenteId,
            UUID entregaId, UUID loteId, LlmResultado resultado) {
        RegistroUsoLlm registro = new RegistroUsoLlm();
        registro.setDocenteId(docenteId);
        registro.setOperacion(operacion);
        registro.setTier(tier);
        registro.setModelo(resultado.modelo());
        registro.setInputTokens(resultado.uso().inputTokens());
        registro.setOutputTokens(resultado.uso().outputTokens());
        registro.setEntregaId(entregaId);
        registro.setLoteId(loteId);
        registros.save(registro);
    }
}
