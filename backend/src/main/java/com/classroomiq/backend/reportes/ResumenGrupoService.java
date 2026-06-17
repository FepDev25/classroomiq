package com.classroomiq.backend.reportes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.entrega.LoteService;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.evaluacion.domain.Evaluacion;
import com.classroomiq.backend.evaluacion.domain.EvaluacionCriterio;
import com.classroomiq.backend.evaluacion.domain.EstadoEvaluacion;
import com.classroomiq.backend.evaluacion.repository.EvaluacionCriterioRepository;
import com.classroomiq.backend.evaluacion.repository.EvaluacionRepository;
import com.classroomiq.backend.reportes.calculo.Estadisticas;
import com.classroomiq.backend.reportes.domain.ResumenGrupo;
import com.classroomiq.backend.reportes.repository.ResumenGrupoRepository;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse.CriterioResumen;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse.EstadisticasNotas;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse.NivelConteo;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse.RangoNota;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;

/**
 * Resumen por grupo de un lote (Fase 5, sección 6). Computa, sobre las evaluaciones APROBADA del
 * lote (congeladas), las estadísticas de notas, el análisis por criterio (promedio y mapa de dominio
 * = distribución por nivel) y los criterios de mayor dificultad.
 *
 * <p>On-demand: las evaluaciones aprobadas no cambian, así que recomputar es barato y siempre refleja
 * el estado actual. La narrativa LLM (Hito 5) será lo único que se persista (caro de regenerar).
 * Acotado al lote del docente: {@link LoteService#cargarPropio} valida la propiedad (404 si ajeno).
 */
@Service
public class ResumenGrupoService {

    private static final int BUCKETS_HISTOGRAMA = 5;
    private static final int MAX_CRITERIOS_DIFICILES = 3;

    private final LoteService loteService;
    private final EntregaRepository entregas;
    private final EvaluacionRepository evaluaciones;
    private final EvaluacionCriterioRepository criterios;
    private final RubricaRepository rubricas;
    private final ResumenGrupoRepository narrativas;

    public ResumenGrupoService(LoteService loteService, EntregaRepository entregas,
            EvaluacionRepository evaluaciones, EvaluacionCriterioRepository criterios,
            RubricaRepository rubricas, ResumenGrupoRepository narrativas) {
        this.loteService = loteService;
        this.entregas = entregas;
        this.evaluaciones = evaluaciones;
        this.criterios = criterios;
        this.rubricas = rubricas;
        this.narrativas = narrativas;
    }

    /**
     * Resumen del lote. Requiere que todas las entregas listas estén evaluadas y aprobadas
     * (el lote "completo", CLAUDE.md sección 6); si no, 422.
     */
    @Transactional(readOnly = true)
    public ResumenGrupoResponse obtener(UUID loteId) {
        return resumir(loteService.cargarPropio(loteId));
    }

    /**
     * Computa el resumen de un lote YA autorizado. El llamador debe haber validado el acceso (el
     * docente dueño vía {@link #obtener}, o un coordinador con la materia asignada). No verifica
     * propiedad: solo agrega las evaluaciones aprobadas.
     */
    @Transactional(readOnly = true)
    public ResumenGrupoResponse resumir(Lote lote) {
        UUID loteId = lote.getId();

        List<UUID> listas = entregas.findAllByLoteId(loteId).stream()
                .filter(e -> e.getEstado() == EstadoEntrega.LISTO)
                .map(Entrega::getId)
                .toList();
        if (listas.isEmpty()) {
            throw new ReglaNegocioException("El lote no tiene entregas listas para resumir");
        }

        List<Evaluacion> aprobadas = evaluaciones.findAllByEntregaIdIn(listas).stream()
                .filter(e -> e.getEstado() == EstadoEvaluacion.APROBADA)
                .toList();
        if (aprobadas.size() < listas.size()) {
            throw new ReglaNegocioException(
                    "El lote no está completo: %d de %d entregas tienen evaluación aprobada"
                            .formatted(aprobadas.size(), listas.size()));
        }

        Rubrica rubrica = cargarRubrica(lote.getRubricaId());
        return construir(lote, rubrica, aprobadas);
    }

    private ResumenGrupoResponse construir(Lote lote, Rubrica rubrica, List<Evaluacion> aprobadas) {
        int total = aprobadas.size();
        double puntajeTotalRubrica = rubrica.getPuntajeTotal().doubleValue();

        List<Double> notas = aprobadas.stream()
                .map(Evaluacion::getPuntajeTotalFinal)
                .filter(p -> p != null)
                .map(BigDecimal::doubleValue)
                .toList();
        if (notas.isEmpty()) {
            throw new ReglaNegocioException("Las evaluaciones aprobadas no tienen puntaje total");
        }
        Estadisticas.Resumen res = Estadisticas.resumir(notas);
        EstadisticasNotas estadisticas = new EstadisticasNotas(res.promedio(), res.mediana(),
                res.minima(), res.maxima(), histograma(notas, puntajeTotalRubrica));

        // Criterios evaluados agrupados por criterio de la rúbrica (de todas las evaluaciones).
        Map<UUID, List<EvaluacionCriterio>> porCriterio = aprobadas.stream()
                .flatMap(e -> criterios.findAllByEvaluacionIdOrderByOrdenAsc(e.getId()).stream())
                .collect(Collectors.groupingBy(EvaluacionCriterio::getCriterioId));

        List<CriterioResumen> resumenes = new ArrayList<>();
        for (Criterio criterio : rubrica.getCriterios()) {
            resumenes.add(resumirCriterio(criterio, porCriterio.getOrDefault(criterio.getId(), List.of()), total));
        }

        // Tabla de mejor a peor desempeño grupal (criterios sin promedio van al final).
        Comparator<CriterioResumen> porDesempeno = Comparator.comparing(CriterioResumen::promedioPct,
                Comparator.nullsLast(Comparator.reverseOrder()));
        List<CriterioResumen> ordenados = resumenes.stream().sorted(porDesempeno).toList();

        List<String> dificiles = ordenados.stream()
                .filter(c -> c.promedioPct() != null)
                .sorted(Comparator.comparingDouble(CriterioResumen::promedioPct))
                .limit(MAX_CRITERIOS_DIFICILES)
                .map(CriterioResumen::nombre)
                .toList();

        String narrativa = narrativas.findByLoteId(lote.getId())
                .map(ResumenGrupo::getNarrativa)
                .orElse(null);

        return new ResumenGrupoResponse(lote.getId(), lote.getNombre(), total, puntajeTotalRubrica,
                estadisticas, ordenados, dificiles, narrativa);
    }

    private CriterioResumen resumirCriterio(Criterio criterio, List<EvaluacionCriterio> ecs, int total) {
        double max = criterio.getPuntajeMaximo().doubleValue();

        List<Double> puntajes = ecs.stream()
                .map(ec -> ec.getPuntajeFinal() != null ? ec.getPuntajeFinal() : ec.getPuntajeSugerido())
                .filter(p -> p != null)
                .map(BigDecimal::doubleValue)
                .toList();
        Double promedio = puntajes.isEmpty() ? null
                : puntajes.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        Double promedioPct = (promedio == null || max == 0.0) ? null : promedio / max * 100.0;

        // Mapa de dominio: cuántos del grupo alcanzaron cada nivel (final si el docente lo fijó).
        Map<UUID, Long> conteoPorNivel = ecs.stream()
                .map(ec -> ec.getNivelFinalId() != null ? ec.getNivelFinalId() : ec.getNivelSugeridoId())
                .filter(id -> id != null)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        List<NivelConteo> distribucion = new ArrayList<>();
        int conNivel = 0;
        for (NivelDesempeno nivel : criterio.getNiveles()) {
            int cantidad = conteoPorNivel.getOrDefault(nivel.getId(), 0L).intValue();
            conNivel += cantidad;
            double pct = total == 0 ? 0.0 : (double) cantidad / total * 100.0;
            distribucion.add(new NivelConteo(nivel.getId(), nivel.getNombre(), cantidad, pct));
        }

        return new CriterioResumen(criterio.getId(), criterio.getNombre(), max, promedio, promedioPct,
                puntajes.size(), distribucion, total - conNivel);
    }

    /** Histograma de notas en {@value #BUCKETS_HISTOGRAMA} rangos iguales sobre [0, puntajeTotal]. */
    private List<RangoNota> histograma(List<Double> notas, double total) {
        double ancho = total > 0 ? total / BUCKETS_HISTOGRAMA : 1.0;
        int[] conteos = new int[BUCKETS_HISTOGRAMA];
        for (double nota : notas) {
            int idx = (int) (nota / ancho);
            if (idx >= BUCKETS_HISTOGRAMA) {
                idx = BUCKETS_HISTOGRAMA - 1; // la nota máxima cae en el último bucket
            }
            if (idx < 0) {
                idx = 0;
            }
            conteos[idx]++;
        }
        List<RangoNota> rangos = new ArrayList<>();
        for (int i = 0; i < BUCKETS_HISTOGRAMA; i++) {
            double min = i * ancho;
            double maxRango = (i + 1) * ancho;
            rangos.add(new RangoNota("%.1f–%.1f".formatted(min, maxRango), min, maxRango, conteos[i]));
        }
        return rangos;
    }

    private Rubrica cargarRubrica(UUID rubricaId) {
        Rubrica rubrica = rubricas.findById(rubricaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rúbrica no encontrada"));
        rubrica.getCriterios().forEach(c -> c.getNiveles().size()); // inicializa el agregado LAZY
        return rubrica;
    }
}
