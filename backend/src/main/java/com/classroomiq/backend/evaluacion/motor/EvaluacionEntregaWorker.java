package com.classroomiq.backend.evaluacion.motor;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.procesamiento.EntregaEstadoEvent;
import com.classroomiq.backend.entrega.repository.EntregaRepository;

/**
 * Worker que evalúa una entrega en background (Fase 4, paso explícito separado del indexado de Fase
 * 3). Bean aparte para que el proxy de {@code @Async} aplique, y reusa el mismo executor, el
 * {@link EntregaEstadoEvent} y el bus SSE que el procesamiento de Fase 3.
 *
 * <p>Job auto-describible: recibe el {@code tenantId} explícito y abre el {@link TenantContext} en
 * un try/finally antes de tocar la BD, igual que {@code ProcesamientoEntregaWorker}. Transición de
 * la entrega ya indexada: LISTO → EVALUANDO → LISTO (o ERROR si el motor falla); cada cambio publica
 * un evento para el stream en tiempo real. El borrador queda persistido por {@link MotorEvaluacion}.
 */
@Service
public class EvaluacionEntregaWorker {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionEntregaWorker.class);
    private static final int MAX_MENSAJE_ERROR = 4000;

    private final EntregaRepository entregas;
    private final MotorEvaluacion motor;
    private final ApplicationEventPublisher eventos;

    public EvaluacionEntregaWorker(EntregaRepository entregas, MotorEvaluacion motor,
            ApplicationEventPublisher eventos) {
        this.entregas = entregas;
        this.motor = motor;
        this.eventos = eventos;
    }

    @Async("procesamientoExecutor")
    public void evaluarEntrega(UUID tenantId, UUID entregaId) {
        TenantContext.set(tenantId);
        try {
            Entrega entrega = entregas.findById(entregaId).orElse(null);
            if (entrega == null) {
                log.warn("Entrega {} no encontrada en tenant {} al evaluar", entregaId, tenantId);
                return;
            }
            transicion(tenantId, entrega, EstadoEntrega.EVALUANDO, null);
            try {
                motor.evaluar(entregaId);
                log.info("Entrega {} evaluada: borrador generado", entregaId);
                transicion(tenantId, entrega, EstadoEntrega.LISTO, null);
            } catch (RuntimeException ex) {
                log.error("Fallo al evaluar la entrega {}", entregaId, ex);
                transicion(tenantId, entrega, EstadoEntrega.ERROR, mensaje(ex));
            }
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

    private String mensaje(RuntimeException ex) {
        String texto = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return texto.length() > MAX_MENSAJE_ERROR ? texto.substring(0, MAX_MENSAJE_ERROR) : texto;
    }
}
