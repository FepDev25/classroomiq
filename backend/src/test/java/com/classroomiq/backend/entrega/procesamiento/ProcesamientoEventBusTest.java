package com.classroomiq.backend.entrega.procesamiento;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.classroomiq.backend.entrega.domain.EstadoEntrega;

import reactor.test.StepVerifier;

/**
 * Bus SSE (Hito 6) sin contexto de Spring: el suscriptor de un lote/tenant recibe solo los eventos
 * que le corresponden. Garantiza el aislamiento — los eventos de otro tenant o de otro lote se
 * filtran y nunca llegan al stream del suscriptor.
 */
class ProcesamientoEventBusTest {

    @Test
    void elSuscriptorRecibeSoloLosEventosDeSuLoteYTenant() {
        ProcesamientoEventBus bus = new ProcesamientoEventBus();

        UUID tenant = UUID.randomUUID();
        UUID otroTenant = UUID.randomUUID();
        UUID lote = UUID.randomUUID();
        UUID otroLote = UUID.randomUUID();
        UUID entrega = UUID.randomUUID();

        // directBestEffort no replica a suscriptores tardíos: emitimos después de suscribir.
        StepVerifier.create(bus.suscribir(lote, tenant))
                .then(() -> {
                    bus.onEstado(new EntregaEstadoEvent(tenant, lote, entrega, EstadoEntrega.PROCESANDO));
                    bus.onEstado(new EntregaEstadoEvent(otroTenant, lote, entrega, EstadoEntrega.PROCESANDO));
                    bus.onEstado(new EntregaEstadoEvent(tenant, otroLote, entrega, EstadoEntrega.PROCESANDO));
                    bus.onEstado(new EntregaEstadoEvent(tenant, lote, entrega, EstadoEntrega.LISTO));
                })
                .assertNext(ev -> {
                    assertThat(ev.entregaId()).isEqualTo(entrega);
                    assertThat(ev.estado()).isEqualTo(EstadoEntrega.PROCESANDO);
                })
                .assertNext(ev -> assertThat(ev.estado()).isEqualTo(EstadoEntrega.LISTO))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}
