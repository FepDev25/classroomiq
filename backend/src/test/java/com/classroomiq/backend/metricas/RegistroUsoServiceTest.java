package com.classroomiq.backend.metricas;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.classroomiq.backend.metricas.domain.OperacionLlm;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.ModeloTier;
import com.classroomiq.backend.provider.llm.UsoTokens;

/**
 * Unit del registro de uso (Fase 6, sin Spring): es fail-open (un fallo del writer no se propaga, una
 * métrica nunca debe tumbar la evaluación) y no registra nada si el proveedor no informó tokens.
 */
class RegistroUsoServiceTest {

    private static final LlmResultado CON_USO =
            new LlmResultado("texto", "claude-sonnet-4-6", "end_turn", new UsoTokens(10, 20));

    @Test
    void fallaAbiertoSiElWriterRevienta() {
        RegistroUsoWriter writer = new RegistroUsoWriter(null) {
            @Override
            public void persistir(OperacionLlm op, ModeloTier tier, UUID docenteId,
                    UUID entregaId, UUID loteId, LlmResultado resultado) {
                throw new RuntimeException("boom de persistencia");
            }
        };
        RegistroUsoService service = new RegistroUsoService(writer);

        assertThatCode(() -> service.registrarEvaluacion(UUID.randomUUID(), UUID.randomUUID(),
                ModeloTier.POTENTE, CON_USO)).doesNotThrowAnyException();
    }

    @Test
    void noRegistraSiNoHayTokens() {
        AtomicInteger llamadas = new AtomicInteger();
        RegistroUsoWriter writer = new RegistroUsoWriter(null) {
            @Override
            public void persistir(OperacionLlm op, ModeloTier tier, UUID docenteId,
                    UUID entregaId, UUID loteId, LlmResultado resultado) {
                llamadas.incrementAndGet();
            }
        };
        RegistroUsoService service = new RegistroUsoService(writer);

        LlmResultado sinUso = new LlmResultado("texto", "claude-haiku-4-5", "end_turn", null);
        service.registrarNarrativa(UUID.randomUUID(), UUID.randomUUID(), ModeloTier.ECONOMICO, sinUso);

        assertThat(llamadas).hasValue(0);
    }

    @Test
    void registraCuandoHayTokens() {
        AtomicInteger llamadas = new AtomicInteger();
        RegistroUsoWriter writer = new RegistroUsoWriter(null) {
            @Override
            public void persistir(OperacionLlm op, ModeloTier tier, UUID docenteId,
                    UUID entregaId, UUID loteId, LlmResultado resultado) {
                llamadas.incrementAndGet();
            }
        };
        RegistroUsoService service = new RegistroUsoService(writer);

        service.registrarEvaluacion(UUID.randomUUID(), UUID.randomUUID(), ModeloTier.POTENTE, CON_USO);

        assertThat(llamadas).hasValue(1);
    }
}
