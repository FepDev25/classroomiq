package com.classroomiq.backend.evaluacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.ConflictoException;
import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.evaluacion.domain.EstadoEvaluacion;
import com.classroomiq.backend.evaluacion.domain.Evaluacion;
import com.classroomiq.backend.evaluacion.domain.EvaluacionCriterio;
import com.classroomiq.backend.evaluacion.dto.BorradorResponse;
import com.classroomiq.backend.evaluacion.dto.CitaResponse;
import com.classroomiq.backend.evaluacion.dto.ComentarioRequest;
import com.classroomiq.backend.evaluacion.dto.CriterioEvaluadoResponse;
import com.classroomiq.backend.evaluacion.dto.CriterioRevisionRequest;
import com.classroomiq.backend.evaluacion.dto.NivelOpcionResponse;
import com.classroomiq.backend.evaluacion.motor.RangoPuntaje;
import com.classroomiq.backend.evaluacion.repository.CitaFragmentoRepository;
import com.classroomiq.backend.evaluacion.repository.EvaluacionCriterioRepository;
import com.classroomiq.backend.evaluacion.repository.EvaluacionRepository;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;

/**
 * Revisión del borrador por el docente: leer el borrador completo, editar cada criterio (puntaje,
 * nivel, justificación, toggle de revisado), fijar el comentario general y aprobar la evaluación.
 *
 * <p>El docente es el juez: el motor sugiere, el docente decide. La aprobación recalcula el total
 * final según el {@code ModoTotal} de la rúbrica y congela la evaluación. Todo con scoping por
 * docente (404 si la evaluación es ajena) y aislamiento por tenant vía {@code @TenantId}.
 */
@Service
public class RevisionService {

    private final EvaluacionRepository evaluaciones;
    private final EvaluacionCriterioRepository criterios;
    private final CitaFragmentoRepository citas;
    private final RubricaRepository rubricas;
    private final AuthContext auth;

    public RevisionService(EvaluacionRepository evaluaciones, EvaluacionCriterioRepository criterios,
            CitaFragmentoRepository citas, RubricaRepository rubricas, AuthContext auth) {
        this.evaluaciones = evaluaciones;
        this.criterios = criterios;
        this.citas = citas;
        this.rubricas = rubricas;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public BorradorResponse obtenerPorEntrega(UUID entregaId) {
        Evaluacion eval = evaluaciones.findByEntregaId(entregaId)
                .filter(e -> e.getDocenteId().equals(auth.requireUserId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("La entrega no tiene borrador de evaluación"));
        return construirBorrador(eval);
    }

    @Transactional
    public CriterioEvaluadoResponse editarCriterio(UUID evaluacionId, UUID criterioEvalId,
            CriterioRevisionRequest request) {
        Evaluacion eval = cargarPropiaEditable(evaluacionId);
        EvaluacionCriterio ec = criterios.findById(criterioEvalId)
                .filter(c -> c.getEvaluacion().getId().equals(eval.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Criterio de evaluación no encontrado"));

        Criterio criterio = criterioDeRubrica(eval.getRubricaId(), ec.getCriterioId());
        NivelDesempeno nivelFinal = validarNivelYPuntaje(criterio, request);

        ec.setNivelFinalId(nivelFinal != null ? nivelFinal.getId() : null);
        ec.setPuntajeFinal(request.puntajeFinal());
        ec.setJustificacionEditada(request.justificacionEditada());
        ec.setRevisadoManual(request.revisadoManual());
        criterios.save(ec);

        return construirCriterio(ec, criterio);
    }

    @Transactional
    public BorradorResponse editarComentario(UUID evaluacionId, ComentarioRequest request) {
        Evaluacion eval = cargarPropiaEditable(evaluacionId);
        eval.setComentarioGeneral(request.comentarioGeneral());
        evaluaciones.save(eval);
        return construirBorrador(eval);
    }

    @Transactional
    public BorradorResponse aprobar(UUID evaluacionId) {
        Evaluacion eval = cargarPropiaEditable(evaluacionId);
        Rubrica rubrica = cargarRubrica(eval.getRubricaId());
        List<EvaluacionCriterio> ecs = criterios.findAllByEvaluacionIdOrderByOrdenAsc(eval.getId());

        eval.setPuntajeTotalFinal(totalFinal(rubrica, ecs));
        eval.setEstado(EstadoEvaluacion.APROBADA);
        eval.setAprobadaAt(Instant.now());
        evaluaciones.save(eval);
        return construirBorrador(eval);
    }

    // --- carga y validación ---

    private Evaluacion cargarPropiaEditable(UUID evaluacionId) {
        Evaluacion eval = evaluaciones.findByIdAndDocenteId(evaluacionId, auth.requireUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Evaluación no encontrada"));
        if (eval.getEstado() == EstadoEvaluacion.APROBADA) {
            throw new ConflictoException("La evaluación ya fue aprobada y no puede modificarse");
        }
        return eval;
    }

    /** Valida que el nivel (si se indica) sea del criterio y que el puntaje caiga en el rango válido. */
    private NivelDesempeno validarNivelYPuntaje(Criterio criterio, CriterioRevisionRequest request) {
        NivelDesempeno nivel = null;
        if (request.nivelFinalId() != null) {
            nivel = criterio.getNiveles().stream()
                    .filter(n -> n.getId().equals(request.nivelFinalId()))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException(
                            "El nivel indicado no pertenece al criterio '" + criterio.getNombre() + "'"));
        }
        BigDecimal puntaje = request.puntajeFinal();
        if (puntaje == null) {
            return nivel;
        }
        if (puntaje.signum() < 0) {
            throw new ReglaNegocioException("El puntaje no puede ser negativo");
        }
        if (nivel != null) {
            RangoPuntaje rango = RangoPuntaje.de(nivel, criterio.getPuntajeMaximo());
            if (!rango.contiene(puntaje)) {
                throw new ReglaNegocioException("El puntaje " + puntaje + " está fuera del rango del nivel '"
                        + nivel.getNombre() + "' [" + rango.min() + "–" + rango.max() + "]");
            }
        } else if (puntaje.compareTo(criterio.getPuntajeMaximo()) > 0) {
            throw new ReglaNegocioException("El puntaje " + puntaje + " supera el máximo del criterio ("
                    + criterio.getPuntajeMaximo() + ")");
        }
        return nivel;
    }

    private Criterio criterioDeRubrica(UUID rubricaId, UUID criterioId) {
        return cargarRubrica(rubricaId).getCriterios().stream()
                .filter(c -> c.getId().equals(criterioId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Criterio no encontrado en la rúbrica"));
    }

    private Rubrica cargarRubrica(UUID rubricaId) {
        Rubrica rubrica = rubricas.findById(rubricaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rúbrica no encontrada"));
        rubrica.getCriterios().forEach(c -> c.getNiveles().size()); // inicializa el agregado
        return rubrica;
    }

    /** Total final por {@code ModoTotal}: usa el puntaje del docente si lo fijó, si no el sugerido. */
    private BigDecimal totalFinal(Rubrica rubrica, List<EvaluacionCriterio> ecs) {
        List<BigDecimal> puntajes = ecs.stream()
                .map(ec -> ec.getPuntajeFinal() != null ? ec.getPuntajeFinal() : ec.getPuntajeSugerido())
                .filter(p -> p != null)
                .toList();
        if (puntajes.isEmpty()) {
            return null;
        }
        BigDecimal suma = puntajes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return switch (rubrica.getModoTotal()) {
            case SUMA -> suma;
            case PROMEDIO -> suma.divide(BigDecimal.valueOf(puntajes.size()), 2, RoundingMode.HALF_UP);
        };
    }

    // --- construcción de respuestas ---

    private BorradorResponse construirBorrador(Evaluacion eval) {
        Rubrica rubrica = cargarRubrica(eval.getRubricaId());
        Map<UUID, Criterio> porId = rubrica.getCriterios().stream()
                .collect(Collectors.toMap(Criterio::getId, Function.identity()));

        List<CriterioEvaluadoResponse> criteriosResp = criterios
                .findAllByEvaluacionIdOrderByOrdenAsc(eval.getId()).stream()
                .map(ec -> construirCriterio(ec, porId.get(ec.getCriterioId())))
                .toList();

        return new BorradorResponse(eval.getId(), eval.getEntregaId(), eval.getRubricaId(), eval.getEstado(),
                eval.getPuntajeTotalSugerido(), eval.getPuntajeTotalFinal(), eval.getComentarioGeneral(),
                eval.getAprobadaAt(), criteriosResp);
    }

    private CriterioEvaluadoResponse construirCriterio(EvaluacionCriterio ec, Criterio criterio) {
        List<NivelOpcionResponse> niveles = criterio == null ? List.of() : criterio.getNiveles().stream()
                .map(n -> {
                    RangoPuntaje r = RangoPuntaje.de(n, criterio.getPuntajeMaximo());
                    return new NivelOpcionResponse(n.getId(), n.getNombre(), n.getDescripcion(),
                            r.min(), r.max(), n.getOrden());
                })
                .toList();

        String nivelSugeridoNombre = criterio == null ? null : nombreNivel(criterio, ec.getNivelSugeridoId());

        List<CitaResponse> citasResp = citas.findAllByEvaluacionCriterioIdOrderByOrdenAsc(ec.getId()).stream()
                .map(c -> new CitaResponse(c.getId(), c.getFragmentoId(), c.getTextoCitado(), c.getOrden()))
                .toList();

        return new CriterioEvaluadoResponse(
                ec.getId(),
                ec.getCriterioId(),
                criterio == null ? null : criterio.getNombre(),
                criterio == null ? null : criterio.getDescripcion(),
                criterio == null ? null : criterio.getPuntajeMaximo(),
                ec.isEvaluable(),
                ec.getNivelSugeridoId(),
                nivelSugeridoNombre,
                ec.getPuntajeSugerido(),
                ec.getJustificacion(),
                ec.getAdvertencia(),
                ec.getNivelFinalId(),
                ec.getPuntajeFinal(),
                ec.getJustificacionEditada(),
                ec.isRevisadoManual(),
                ec.getOrden(),
                niveles,
                citasResp);
    }

    private String nombreNivel(Criterio criterio, UUID nivelId) {
        if (nivelId == null) {
            return null;
        }
        return criterio.getNiveles().stream()
                .filter(n -> n.getId().equals(nivelId))
                .map(NivelDesempeno::getNombre)
                .findFirst()
                .orElse(null);
    }
}
