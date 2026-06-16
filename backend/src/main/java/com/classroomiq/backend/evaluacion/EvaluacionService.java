package com.classroomiq.backend.evaluacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.entrega.LoteService;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.evaluacion.motor.EvaluacionEntregaWorker;

/**
 * Dispara la evaluación en background de las entregas de un lote. Verifica la propiedad del lote
 * (vía {@link LoteService}, 404 si es ajeno) y encola un job por cada entrega ya indexada (estado
 * LISTO) con su {@code tenantId} explícito.
 *
 * <p>La evaluación es un paso explícito separado del indexado: solo se evalúan entregas que ya
 * terminaron de indexarse. Reevaluar es idempotente — el motor borra el borrador previo. No es
 * transaccional: las entregas ya están comprometidas; el worker corre en otro hilo/transacción.
 */
@Service
public class EvaluacionService {

    private final LoteService loteService;
    private final EntregaRepository entregas;
    private final EvaluacionEntregaWorker worker;
    private final AuthContext auth;

    public EvaluacionService(LoteService loteService, EntregaRepository entregas,
            EvaluacionEntregaWorker worker, AuthContext auth) {
        this.loteService = loteService;
        this.entregas = entregas;
        this.worker = worker;
        this.auth = auth;
    }

    /**
     * Encola la evaluación de las entregas indexadas del lote.
     *
     * @return cantidad de entregas encoladas
     */
    public int evaluarLote(UUID loteId) {
        UUID tenantId = auth.requireTenantId();
        loteService.cargarPropio(loteId); // valida tenant + docente; 404 si ajeno

        List<Entrega> aEvaluar = entregas.findAllByLoteId(loteId).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.LISTO)
                .toList();

        for (Entrega entrega : aEvaluar) {
            worker.evaluarEntrega(tenantId, entrega.getId());
        }
        return aEvaluar.size();
    }
}
