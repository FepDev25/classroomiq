package com.classroomiq.backend.entrega.procesamiento;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.EstadoLote;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.repository.ArchivoEntregaRepository;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;

/**
 * Worker que procesa una entrega en background. Es un bean separado para que el proxy de
 * {@code @Async} aplique (no funciona en auto-invocación).
 *
 * <p>Job auto-describible: recibe el {@code tenantId} explícito y abre el {@link TenantContext} en
 * un try/finally <strong>antes</strong> de tocar la BD; así el {@code @TenantId} de Hibernate
 * estampa/filtra el tenant correcto en el hilo del executor. Máquina de estados por entrega
 * (PENDIENTE→PROCESANDO→LISTO/ERROR); un error en una entrega no detiene a las demás del lote.
 */
@Service
public class ProcesamientoEntregaWorker {

    private static final Logger log = LoggerFactory.getLogger(ProcesamientoEntregaWorker.class);
    private static final int MAX_MENSAJE_ERROR = 4000;

    private final EntregaRepository entregas;
    private final ArchivoEntregaRepository archivos;
    private final LoteRepository lotes;
    private final ProcesadorEntrega procesador;
    private final ApplicationEventPublisher eventos;

    public ProcesamientoEntregaWorker(EntregaRepository entregas, ArchivoEntregaRepository archivos,
            LoteRepository lotes, ProcesadorEntrega procesador, ApplicationEventPublisher eventos) {
        this.entregas = entregas;
        this.archivos = archivos;
        this.lotes = lotes;
        this.procesador = procesador;
        this.eventos = eventos;
    }

    @Async("procesamientoExecutor")
    public void procesarEntrega(UUID tenantId, UUID entregaId) {
        TenantContext.set(tenantId);
        try {
            Entrega entrega = entregas.findById(entregaId).orElse(null);
            if (entrega == null) {
                log.warn("Entrega {} no encontrada en tenant {}", entregaId, tenantId);
                return;
            }
            transicion(tenantId, entrega, EstadoEntrega.PROCESANDO, null);
            try {
                List<ArchivoEntrega> archivosEntrega =
                        archivos.findAllByEntrega_IdOrderByOrdenAsc(entregaId);
                int fragmentos = procesador.indexar(entrega, archivosEntrega);
                log.info("Entrega {} indexada: {} fragmentos", entregaId, fragmentos);
                transicion(tenantId, entrega, EstadoEntrega.LISTO, null);
            } catch (RuntimeException ex) {
                log.error("Fallo al procesar la entrega {}", entregaId, ex);
                transicion(tenantId, entrega, EstadoEntrega.ERROR, mensaje(ex));
            }
            actualizarEstadoLote(entrega.getLoteId());
        } finally {
            TenantContext.clear();
        }
    }

    private void transicion(UUID tenantId, Entrega entrega, EstadoEntrega estado, String error) {
        entrega.setEstado(estado);
        entrega.setMensajeError(error);
        entregas.save(entrega);
        eventos.publishEvent(new EntregaEstadoEvent(tenantId, entrega.getLoteId(), entrega.getId(), estado));
    }

    /** Marca el lote como LISTO cuando todas sus entregas terminaron (LISTO o ERROR). */
    private void actualizarEstadoLote(UUID loteId) {
        boolean todasTerminadas = entregas.findAllByLoteId(loteId).stream()
                .allMatch(e -> e.getEstado() == EstadoEntrega.LISTO || e.getEstado() == EstadoEntrega.ERROR);
        if (todasTerminadas) {
            lotes.findById(loteId).ifPresent(lote -> {
                lote.setEstado(EstadoLote.LISTO);
                lotes.save(lote);
            });
        }
    }

    private String mensaje(RuntimeException ex) {
        String texto = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return texto.length() > MAX_MENSAJE_ERROR ? texto.substring(0, MAX_MENSAJE_ERROR) : texto;
    }
}
