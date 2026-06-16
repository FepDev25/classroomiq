package com.classroomiq.backend.similitud;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.entrega.LoteService;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.similitud.calculo.MatchFragmento;
import com.classroomiq.backend.similitud.calculo.ParCalculado;
import com.classroomiq.backend.similitud.calculo.ParTextual;
import com.classroomiq.backend.similitud.calculo.SimilitudSemanticaService;
import com.classroomiq.backend.similitud.calculo.SimilitudTextualService;
import com.classroomiq.backend.similitud.domain.FragmentoParSimilar;
import com.classroomiq.backend.similitud.domain.ParSimilitud;
import com.classroomiq.backend.similitud.domain.ReporteSimilitud;
import com.classroomiq.backend.similitud.dto.FragmentoSimilarResponse;
import com.classroomiq.backend.similitud.dto.ParSimilitudResponse;
import com.classroomiq.backend.similitud.dto.ReporteSimilitudResponse;
import com.classroomiq.backend.similitud.repository.ParSimilitudRepository;
import com.classroomiq.backend.similitud.repository.ReporteSimilitudRepository;

/**
 * Detección de similitud entre las entregas de un lote (Fase 5). Genera el reporte (cálculo
 * semántico + textual, fusión y persistencia) y lo consulta. Acotado al lote del docente autenticado
 * (mismo docente/materia): {@link LoteService#cargarPropio} valida la propiedad (404 si es ajeno) y
 * el aislamiento por tenant lo aplica {@code @TenantId}.
 *
 * <p>El reporte se persiste: el cálculo de pares es O(n²) y el resultado es estable. Regenerar es
 * idempotente — borra el reporte previo antes de recalcular.
 */
@Service
public class SimilitudService {

    /** Mensaje fijo no-acusatorio que acompaña el reporte (CLAUDE.md sección 5). */
    static final String AVISO =
            "El reporte señala similitud entre entregas para revisión manual del docente. "
                    + "El sistema no determina si existe deshonestidad académica.";

    private final LoteService loteService;
    private final EntregaRepository entregas;
    private final SimilitudSemanticaService semantica;
    private final SimilitudTextualService textual;
    private final ReporteSimilitudRepository reportes;
    private final ParSimilitudRepository pares;
    private final SimilitudProperties properties;

    public SimilitudService(LoteService loteService, EntregaRepository entregas,
            SimilitudSemanticaService semantica, SimilitudTextualService textual,
            ReporteSimilitudRepository reportes, ParSimilitudRepository pares,
            SimilitudProperties properties) {
        this.loteService = loteService;
        this.entregas = entregas;
        this.semantica = semantica;
        this.textual = textual;
        this.reportes = reportes;
        this.pares = pares;
        this.properties = properties;
    }

    /**
     * Genera (o regenera) el reporte de similitud del lote sobre sus entregas en estado LISTO.
     * Requiere al menos dos entregas listas. Si {@code umbral} es {@code null} usa el default.
     */
    @Transactional
    public ReporteSimilitudResponse generar(UUID loteId, BigDecimal umbral) {
        Lote lote = loteService.cargarPropio(loteId);

        List<UUID> listas = entregas.findAllByLoteId(loteId).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.LISTO)
                .map(Entrega::getId)
                .toList();
        if (listas.size() < 2) {
            throw new ReglaNegocioException(
                    "Se necesitan al menos 2 entregas listas en el lote para calcular similitud");
        }

        BigDecimal umbralEfectivo = umbral != null ? umbral : properties.umbralDefault();

        // Regeneración idempotente. flush explícito: en una misma transacción Hibernate emite los
        // INSERT antes que los DELETE, lo que chocaría con uq_repsim_lote al reinsertar el reporte.
        reportes.deleteByLoteId(loteId);
        reportes.flush();

        List<ParCalculado> semanticos = semantica.calcular(listas);
        Map<ParClave, ParTextual> textualesPorPar = indexar(textual.calcular(listas));

        ReporteSimilitud reporte = new ReporteSimilitud();
        reporte.setDocenteId(lote.getDocenteId());
        reporte.setMateriaId(lote.getMateriaId());
        reporte.setLoteId(loteId);
        reporte.setUmbral(umbralEfectivo);
        reporte.setGeneradoAt(Instant.now());

        for (ParCalculado sem : semanticos) {
            ParTextual tex = textualesPorPar.get(new ParClave(sem.entregaAId(), sem.entregaBId()));
            reporte.addPar(construirPar(sem, tex, umbralEfectivo));
        }

        reportes.save(reporte);
        return obtener(loteId);
    }

    /** Devuelve el reporte de similitud del lote; 404 si aún no se generó. */
    @Transactional(readOnly = true)
    public ReporteSimilitudResponse obtener(UUID loteId) {
        loteService.cargarPropio(loteId); // valida tenant + docente
        ReporteSimilitud reporte = reportes.findByLoteId(loteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El lote no tiene reporte de similitud; genéralo primero"));

        Map<UUID, String> rotulos = new LinkedHashMap<>();
        for (Entrega e : entregas.findAllByLoteId(loteId)) {
            rotulos.put(e.getId(), e.getIdentificadorEstudiante());
        }

        List<ParSimilitudResponse> paresResp =
                pares.findAllByReporteIdOrderBySimilitudSemanticaDesc(reporte.getId()).stream()
                        .map(this::toParResponse)
                        .toList();

        List<ReporteSimilitudResponse.EntregaResumen> resumenes = rotulos.entrySet().stream()
                .map(en -> new ReporteSimilitudResponse.EntregaResumen(en.getKey(), en.getValue()))
                .toList();

        return new ReporteSimilitudResponse(loteId, reporte.getUmbral().doubleValue(),
                reporte.getGeneradoAt(), AVISO, resumenes, paresResp);
    }

    private ParSimilitud construirPar(ParCalculado sem, ParTextual tex, BigDecimal umbral) {
        ParSimilitud par = new ParSimilitud();
        par.setEntregaAId(sem.entregaAId());
        par.setEntregaBId(sem.entregaBId());
        par.setSimilitudSemantica(escalar(sem.similitudSemantica()));
        par.setSimilitudTextual(tex != null && tex.similitudTextual() != null
                ? escalar(tex.similitudTextual()) : null);
        par.setSuperaUmbral(par.getSimilitudSemantica().compareTo(umbral) >= 0);

        int orden = 0;
        for (MatchFragmento m : sem.fragmentos()) {
            par.addFragmento(toFragmento(m, orden++));
        }
        if (tex != null) {
            for (MatchFragmento m : tex.fragmentos()) {
                par.addFragmento(toFragmento(m, orden++));
            }
        }
        return par;
    }

    private FragmentoParSimilar toFragmento(MatchFragmento m, int orden) {
        FragmentoParSimilar f = new FragmentoParSimilar();
        f.setTipo(m.tipo());
        f.setFragmentoAId(m.fragmentoAId());
        f.setFragmentoBId(m.fragmentoBId());
        f.setTextoA(m.textoA());
        f.setTextoB(m.textoB());
        f.setSimilitud(escalar(m.similitud()));
        f.setOrden(orden);
        return f;
    }

    private ParSimilitudResponse toParResponse(ParSimilitud par) {
        List<FragmentoSimilarResponse> frags = par.getFragmentos().stream()
                .map(f -> new FragmentoSimilarResponse(f.getTipo(), f.getFragmentoAId(), f.getTextoA(),
                        f.getFragmentoBId(), f.getTextoB(), f.getSimilitud().doubleValue()))
                .toList();
        return new ParSimilitudResponse(par.getEntregaAId(), par.getEntregaBId(),
                par.getSimilitudSemantica().doubleValue(),
                par.getSimilitudTextual() != null ? par.getSimilitudTextual().doubleValue() : null,
                par.isSuperaUmbral(), frags);
    }

    private static BigDecimal escalar(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }

    private Map<ParClave, ParTextual> indexar(List<ParTextual> textuales) {
        Map<ParClave, ParTextual> map = new LinkedHashMap<>();
        for (ParTextual t : textuales) {
            map.put(new ParClave(t.entregaAId(), t.entregaBId()), t);
        }
        return map;
    }

    /** Clave de un par no ordenado de entregas para fusionar los resultados semántico y textual. */
    private record ParClave(UUID a, UUID b) {
    }
}
