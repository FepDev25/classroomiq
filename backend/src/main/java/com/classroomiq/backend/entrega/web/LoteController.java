package com.classroomiq.backend.entrega.web;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.entrega.LoteService;
import com.classroomiq.backend.entrega.dto.EntregaEventoResponse;
import com.classroomiq.backend.entrega.dto.LoteRequest;
import com.classroomiq.backend.entrega.dto.LoteResponse;
import com.classroomiq.backend.entrega.dto.ProcesamientoResponse;
import com.classroomiq.backend.entrega.procesamiento.ProcesamientoEventBus;
import com.classroomiq.backend.entrega.procesamiento.ProcesamientoService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/lotes")
@PreAuthorize("hasRole('DOCENTE')")
public class LoteController {

    /** Ping periódico para mantener viva la conexión SSE frente a proxies (Cloudflare Tunnel). */
    private static final Duration KEEP_ALIVE = Duration.ofSeconds(15);

    private final LoteService lotes;
    private final ProcesamientoService procesamiento;
    private final ProcesamientoEventBus eventBus;
    private final AuthContext auth;

    public LoteController(LoteService lotes, ProcesamientoService procesamiento,
            ProcesamientoEventBus eventBus, AuthContext auth) {
        this.lotes = lotes;
        this.procesamiento = procesamiento;
        this.eventBus = eventBus;
        this.auth = auth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoteResponse crear(@Valid @RequestBody LoteRequest request) {
        return lotes.crear(request);
    }

    @GetMapping
    public List<LoteResponse> listar() {
        return lotes.listar();
    }

    @GetMapping("/{id}")
    public LoteResponse obtener(@PathVariable UUID id) {
        return lotes.obtener(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        lotes.eliminar(id);
    }

    @PostMapping("/{id}/procesar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProcesamientoResponse procesar(@PathVariable UUID id) {
        return new ProcesamientoResponse(procesamiento.iniciar(id));
    }

    /**
     * Stream SSE con el progreso de procesamiento de las entregas del lote. La autorización ocurre
     * aquí, en el hilo del request (donde el {@code TenantContext} y la identidad del JWT siguen
     * disponibles): {@code cargarPropio} valida tenant + docente y devuelve 404 si el lote es ajeno.
     * El {@code tenantId} se captura por valor para filtrar el stream; el hilo reactivo no consulta
     * el dominio ni usa el ThreadLocal.
     */
    @GetMapping(value = "/{id}/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EntregaEventoResponse>> eventos(@PathVariable UUID id) {
        lotes.cargarPropio(id);
        UUID tenantId = auth.requireTenantId();

        Flux<ServerSentEvent<EntregaEventoResponse>> estados = eventBus.suscribir(id, tenantId)
                .map(ev -> ServerSentEvent
                        .builder(new EntregaEventoResponse(ev.entregaId(), ev.estado()))
                        .event("estado-entrega")
                        .build());
        Flux<ServerSentEvent<EntregaEventoResponse>> keepAlive = Flux.interval(KEEP_ALIVE)
                .map(tick -> ServerSentEvent.<EntregaEventoResponse>builder()
                        .comment("keep-alive")
                        .build());
        return Flux.merge(estados, keepAlive);
    }
}
