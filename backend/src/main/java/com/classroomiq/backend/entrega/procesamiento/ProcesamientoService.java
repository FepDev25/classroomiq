package com.classroomiq.backend.entrega.procesamiento;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.entrega.LoteService;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.EstadoLote;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;

/**
 * Dispara el procesamiento en background de las entregas de un lote. Verifica la propiedad del
 * lote (vía {@link LoteService}), persiste el lote en PROCESANDO y encola cada entrega pendiente o
 * con error con su {@code tenantId} explícito.
 *
 * <p>No es transaccional a propósito: los estados se persisten con commits propios <strong>antes</strong>
 * de encolar, de modo que el worker (otro hilo, otra transacción) vea datos ya comprometidos.
 */
@Service
public class ProcesamientoService {

    private final LoteService loteService;
    private final LoteRepository lotes;
    private final EntregaRepository entregas;
    private final ProcesamientoEntregaWorker worker;
    private final AuthContext auth;

    public ProcesamientoService(LoteService loteService, LoteRepository lotes, EntregaRepository entregas,
            ProcesamientoEntregaWorker worker, AuthContext auth) {
        this.loteService = loteService;
        this.lotes = lotes;
        this.entregas = entregas;
        this.worker = worker;
        this.auth = auth;
    }

    /**
     * Encola el procesamiento de las entregas pendientes/erróneas del lote.
     *
     * @return cantidad de entregas encoladas
     */
    public int iniciar(UUID loteId) {
        UUID tenantId = auth.requireTenantId();
        Lote lote = loteService.cargarPropio(loteId);

        List<Entrega> aProcesar = entregas.findAllByLoteId(loteId).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.PENDIENTE || e.getEstado() == EstadoEntrega.ERROR)
                .toList();

        lote.setEstado(EstadoLote.PROCESANDO);
        lotes.save(lote);

        for (Entrega entrega : aProcesar) {
            entrega.setEstado(EstadoEntrega.PENDIENTE);
            entrega.setMensajeError(null);
            entregas.save(entrega);
            worker.procesarEntrega(tenantId, entrega.getId());
        }
        return aProcesar.size();
    }
}
