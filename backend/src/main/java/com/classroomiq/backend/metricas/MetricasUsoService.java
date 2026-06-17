package com.classroomiq.backend.metricas;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.metricas.costo.CalculadoraCosto;
import com.classroomiq.backend.metricas.domain.OperacionLlm;
import com.classroomiq.backend.metricas.dto.DocenteUsoDetalleResponse;
import com.classroomiq.backend.metricas.dto.DocenteUsoResponse;
import com.classroomiq.backend.metricas.dto.MetricasUsoResponse;
import com.classroomiq.backend.metricas.dto.UsoModeloResponse;
import com.classroomiq.backend.metricas.dto.UsoOperacionResponse;
import com.classroomiq.backend.metricas.repository.RegistroUsoLlmRepository;
import com.classroomiq.backend.metricas.repository.UsoDocenteModelo;
import com.classroomiq.backend.metricas.repository.UsoModeloOperacion;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Métricas de uso/costo del LLM para el portal admin (Fase 6). Agrega el libro mayor de tokens por
 * docente y por mes, le imputa costo con {@link CalculadoraCosto} y arma el resumen institucional
 * con la bandera de umbral superado. Aislamiento por tenant garantizado por {@code @TenantId}: un
 * admin solo ve el uso de su institución.
 *
 * <p>El mes se acota en UTC ({@code [primer día, primer día del mes siguiente)}); el costo es una
 * estimación derivada de tarifas configurables, no un valor persistido.
 */
@Service
public class MetricasUsoService {

    private final RegistroUsoLlmRepository registros;
    private final UsuarioRepository usuarios;
    private final CalculadoraCosto calculadora;

    public MetricasUsoService(RegistroUsoLlmRepository registros, UsuarioRepository usuarios,
            CalculadoraCosto calculadora) {
        this.registros = registros;
        this.usuarios = usuarios;
        this.calculadora = calculadora;
    }

    /** Resumen de uso/costo de la institución para el mes dado ({@code null} = mes actual). */
    @Transactional(readOnly = true)
    public MetricasUsoResponse resumen(String mesParam) {
        Rango rango = rango(mesParam);
        List<UsoDocenteModelo> filas = registros.agregarPorDocenteModelo(rango.desde(), rango.hasta());

        // Acumula por docente, sumando el costo modelo por modelo (cada uno con su tarifa).
        Map<UUID, Acumulador> porDocente = new LinkedHashMap<>();
        for (UsoDocenteModelo fila : filas) {
            Acumulador acc = porDocente.computeIfAbsent(fila.getDocenteId(), id -> new Acumulador());
            acc.sumar(fila.getInputTokens(), fila.getOutputTokens(),
                    calculadora.costo(fila.getModelo(), fila.getInputTokens(), fila.getOutputTokens()));
        }

        Map<UUID, Usuario> docentes = cargarDocentes(porDocente.keySet());
        List<DocenteUsoResponse> filasDocente = porDocente.entrySet().stream()
                .map(e -> {
                    Usuario u = docentes.get(e.getKey());
                    Acumulador a = e.getValue();
                    return new DocenteUsoResponse(e.getKey(),
                            u != null ? u.getNombre() : null,
                            u != null ? u.getEmail() : null,
                            a.inputTokens, a.outputTokens, a.inputTokens + a.outputTokens, a.costo);
                })
                .sorted(Comparator.comparing(DocenteUsoResponse::costoEstimado).reversed())
                .toList();

        BigDecimal costoTotal = filasDocente.stream()
                .map(DocenteUsoResponse::costoEstimado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalIn = filasDocente.stream().mapToLong(DocenteUsoResponse::inputTokens).sum();
        long totalOut = filasDocente.stream().mapToLong(DocenteUsoResponse::outputTokens).sum();
        boolean superado = costoTotal.compareTo(calculadora.umbralMensual()) > 0;

        return new MetricasUsoResponse(rango.mes(), calculadora.moneda(), calculadora.umbralMensual(),
                superado, costoTotal, totalIn, totalOut, filasDocente);
    }

    /** Detalle de uso/costo de un docente (por modelo y por operación) en el mes dado. */
    @Transactional(readOnly = true)
    public DocenteUsoDetalleResponse detalleDocente(UUID docenteId, String mesParam) {
        Usuario docente = usuarios.findById(docenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado: " + docenteId));
        Rango rango = rango(mesParam);
        List<UsoModeloOperacion> filas =
                registros.agregarPorModeloOperacion(docenteId, rango.desde(), rango.hasta());

        Map<String, Acumulador> porModelo = new LinkedHashMap<>();
        Map<OperacionLlm, Acumulador> porOperacion = new LinkedHashMap<>();
        for (UsoModeloOperacion fila : filas) {
            BigDecimal costo = calculadora.costo(fila.getModelo(), fila.getInputTokens(), fila.getOutputTokens());
            porModelo.computeIfAbsent(fila.getModelo(), m -> new Acumulador())
                    .sumar(fila.getInputTokens(), fila.getOutputTokens(), costo);
            porOperacion.computeIfAbsent(fila.getOperacion(), o -> new Acumulador())
                    .sumar(fila.getInputTokens(), fila.getOutputTokens(), costo);
        }

        List<UsoModeloResponse> modelos = porModelo.entrySet().stream()
                .map(e -> new UsoModeloResponse(e.getKey(), e.getValue().inputTokens, e.getValue().outputTokens,
                        e.getValue().inputTokens + e.getValue().outputTokens, e.getValue().costo))
                .toList();
        List<UsoOperacionResponse> operaciones = porOperacion.entrySet().stream()
                .map(e -> new UsoOperacionResponse(e.getKey(), e.getValue().inputTokens, e.getValue().outputTokens,
                        e.getValue().inputTokens + e.getValue().outputTokens, e.getValue().costo))
                .toList();

        long totalIn = modelos.stream().mapToLong(UsoModeloResponse::inputTokens).sum();
        long totalOut = modelos.stream().mapToLong(UsoModeloResponse::outputTokens).sum();
        BigDecimal costoTotal = modelos.stream()
                .map(UsoModeloResponse::costoEstimado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DocenteUsoDetalleResponse(docenteId, docente.getNombre(), docente.getEmail(),
                rango.mes(), calculadora.moneda(), totalIn, totalOut, costoTotal, modelos, operaciones);
    }

    private Map<UUID, Usuario> cargarDocentes(Iterable<UUID> ids) {
        List<UUID> lista = new ArrayList<>();
        ids.forEach(lista::add);
        return usuarios.findAllById(lista).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u));
    }

    private Rango rango(String mesParam) {
        YearMonth ym;
        if (mesParam == null || mesParam.isBlank()) {
            ym = YearMonth.now(ZoneOffset.UTC);
        } else {
            try {
                ym = YearMonth.parse(mesParam.trim());
            } catch (DateTimeParseException e) {
                throw new ReglaNegocioException("Mes inválido (formato esperado YYYY-MM): " + mesParam);
            }
        }
        Instant desde = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant hasta = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new Rango(desde, hasta, ym.toString());
    }

    private record Rango(Instant desde, Instant hasta, String mes) {
    }

    /** Acumulador mutable de tokens y costo durante la agregación. */
    private static final class Acumulador {
        private long inputTokens;
        private long outputTokens;
        private BigDecimal costo = BigDecimal.ZERO;

        void sumar(long in, long out, BigDecimal costoParcial) {
            this.inputTokens += in;
            this.outputTokens += out;
            this.costo = this.costo.add(costoParcial);
        }
    }
}
