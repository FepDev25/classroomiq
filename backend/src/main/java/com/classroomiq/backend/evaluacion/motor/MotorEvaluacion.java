package com.classroomiq.backend.evaluacion.motor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.entrega.repository.FragmentoSimilar;
import com.classroomiq.backend.evaluacion.domain.CitaFragmento;
import com.classroomiq.backend.evaluacion.domain.Evaluacion;
import com.classroomiq.backend.evaluacion.domain.EvaluacionCriterio;
import com.classroomiq.backend.evaluacion.repository.EvaluacionRepository;
import com.classroomiq.backend.evaluacion.retrieval.RetrievalCriterioService;
import com.classroomiq.backend.metricas.RegistroUsoService;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.ModoTotal;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.provider.llm.LlmProvider;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.LlmSolicitud;
import com.classroomiq.backend.provider.llm.ModeloTier;

/**
 * Motor de evaluación: genera el borrador de una entrega criterio por criterio (el "asistente que
 * prepara la evaluación", nunca el que pone la nota). Para cada criterio evaluable por contenido
 * recupera los fragmentos relevantes (RAG por criterio), pide al LLM un nivel + puntaje + evidencia,
 * valida que el puntaje caiga en el rango del nivel y persiste el resultado.
 *
 * <p>Los criterios con {@code evaluablePorContenido=false} (demo, exposición) se crean en blanco
 * marcados para el juicio del docente. El total proyectado se calcula según el {@code ModoTotal} de
 * la rúbrica sobre los criterios que sí recibieron puntaje.
 *
 * <p>No es {@code @Transactional} a propósito: las llamadas al LLM son lentas y externas, así que el
 * contexto se lee en una tx aparte ({@link ContextoEvaluacionLoader}) y la persistencia ocurre en la
 * tx propia del {@code save} final. Requiere que el {@code TenantContext} esté fijado por el llamador
 * (el worker de fondo del Hito 4); el aislamiento se apoya en {@code @TenantId}.
 */
@Service
public class MotorEvaluacion {

    private static final int MAX_CITAS = 6;

    private final ContextoEvaluacionLoader contextoLoader;
    private final RetrievalCriterioService retrieval;
    private final FragmentoEntregaRepository fragmentos;
    private final LlmProvider llm;
    private final PromptEvaluacion prompt;
    private final RespuestaEvaluacionParser parser;
    private final EvaluacionRepository evaluaciones;
    private final RegistroUsoService registroUso;

    public MotorEvaluacion(ContextoEvaluacionLoader contextoLoader, RetrievalCriterioService retrieval,
            FragmentoEntregaRepository fragmentos, LlmProvider llm, PromptEvaluacion prompt,
            RespuestaEvaluacionParser parser, EvaluacionRepository evaluaciones,
            RegistroUsoService registroUso) {
        this.contextoLoader = contextoLoader;
        this.retrieval = retrieval;
        this.fragmentos = fragmentos;
        this.llm = llm;
        this.prompt = prompt;
        this.parser = parser;
        this.evaluaciones = evaluaciones;
        this.registroUso = registroUso;
    }

    /**
     * Genera (o regenera) el borrador de evaluación de la entrega y lo persiste. Idempotente: borra
     * el borrador previo antes de crear el nuevo.
     *
     * @return la evaluación persistida (estado BORRADOR)
     */
    public Evaluacion evaluar(UUID entregaId) {
        ContextoEvaluacion ctx = contextoLoader.cargar(entregaId);
        boolean hayContenido = fragmentos.countByEntregaId(entregaId) > 0;

        // Reevaluación desde cero: borra el borrador anterior (criterios y citas en cascada).
        evaluaciones.deleteByEntregaId(entregaId);

        Evaluacion eval = new Evaluacion();
        eval.setDocenteId(ctx.docenteId());
        eval.setEntregaId(entregaId);
        eval.setRubricaId(ctx.rubricaId());

        for (Criterio criterio : ctx.criterios()) {
            eval.addCriterio(evaluarCriterio(ctx.docenteId(), entregaId, criterio, hayContenido));
        }

        eval.setPuntajeTotalSugerido(calcularTotal(ctx.modoTotal(), eval.getCriterios()));
        return evaluaciones.save(eval);
    }

    private EvaluacionCriterio evaluarCriterio(UUID docenteId, UUID entregaId, Criterio criterio,
            boolean hayContenido) {
        EvaluacionCriterio ec = new EvaluacionCriterio();
        ec.setCriterioId(criterio.getId());
        ec.setOrden(criterio.getOrden());

        if (!criterio.isEvaluablePorContenido()) {
            ec.setEvaluable(false);
            ec.setAdvertencia("Criterio no evaluable por contenido: requiere el juicio del docente.");
            return ec;
        }

        ec.setEvaluable(true);
        if (!hayContenido) {
            ec.setAdvertencia("La entrega no tiene contenido indexado para evaluar este criterio.");
            return ec;
        }

        List<FragmentoSimilar> relevantes = retrieval.recuperarParaCriterio(entregaId, criterio);
        LlmResultado resultado = llm.generar(new LlmSolicitud(
                ModeloTier.POTENTE, prompt.system(), prompt.usuario(criterio, relevantes)));
        registroUso.registrarEvaluacion(docenteId, entregaId, ModeloTier.POTENTE, resultado);
        EvaluacionLlmRespuesta respuesta = parser.parsear(resultado.texto());
        aplicarRespuesta(ec, criterio, respuesta, relevantes);
        return ec;
    }

    /** Vuelca la respuesta del LLM al criterio: nivel, puntaje acotado al rango, justificación y citas. */
    private void aplicarRespuesta(EvaluacionCriterio ec, Criterio criterio,
            EvaluacionLlmRespuesta respuesta, List<FragmentoSimilar> relevantes) {
        ec.setJustificacion(respuesta.justificacion());

        StringBuilder advertencia = new StringBuilder();
        if (respuesta.advertencia() != null && !respuesta.advertencia().isBlank()
                && !"null".equalsIgnoreCase(respuesta.advertencia().trim())) {
            advertencia.append(respuesta.advertencia().trim());
        }

        Optional<NivelDesempeno> nivel = emparejarNivel(criterio, respuesta.nivel());
        if (nivel.isPresent()) {
            RangoPuntaje rango = RangoPuntaje.de(nivel.get(), criterio.getPuntajeMaximo());
            BigDecimal ajustado = rango.ajustar(respuesta.puntaje());
            ec.setNivelSugeridoId(nivel.get().getId());
            ec.setPuntajeSugerido(ajustado);
            if (respuesta.puntaje() != null && !rango.contiene(respuesta.puntaje())) {
                anexar(advertencia, "El puntaje propuesto (" + respuesta.puntaje()
                        + ") estaba fuera del rango del nivel y se acotó a " + ajustado + ".");
            }
        } else {
            anexar(advertencia, "El nivel propuesto por el modelo ('" + respuesta.nivel()
                    + "') no coincide con ningún nivel de la rúbrica; requiere revisión del docente.");
        }

        if (advertencia.length() > 0) {
            ec.setAdvertencia(advertencia.toString());
        }
        agregarCitas(ec, respuesta.citas(), relevantes);
    }

    private void agregarCitas(EvaluacionCriterio ec, List<String> citas, List<FragmentoSimilar> relevantes) {
        if (citas == null) {
            return;
        }
        int orden = 0;
        for (String texto : citas) {
            if (texto == null || texto.isBlank() || orden >= MAX_CITAS) {
                continue;
            }
            CitaFragmento cita = new CitaFragmento();
            cita.setTextoCitado(texto.strip());
            cita.setOrden(orden++);
            emparejarFragmento(texto, relevantes).ifPresent(f -> cita.setFragmentoId(f.getId()));
            ec.addCita(cita);
        }
    }

    /** Empareja una cita con el fragmento de origen por contención textual (tolerante a espacios). */
    private Optional<FragmentoSimilar> emparejarFragmento(String cita, List<FragmentoSimilar> relevantes) {
        String objetivo = normalizar(cita);
        if (objetivo.isBlank()) {
            return Optional.empty();
        }
        return relevantes.stream()
                .filter(f -> normalizar(f.getContenido()).contains(objetivo))
                .findFirst();
    }

    private Optional<NivelDesempeno> emparejarNivel(Criterio criterio, String nombreNivel) {
        if (nombreNivel == null || nombreNivel.isBlank()) {
            return Optional.empty();
        }
        String objetivo = nombreNivel.strip();
        return criterio.getNiveles().stream()
                .filter(n -> n.getNombre().equalsIgnoreCase(objetivo))
                .findFirst();
    }

    /** Total proyectado sobre los criterios que recibieron puntaje, según el modo de la rúbrica. */
    private BigDecimal calcularTotal(ModoTotal modo, List<EvaluacionCriterio> criterios) {
        List<BigDecimal> puntajes = criterios.stream()
                .filter(EvaluacionCriterio::isEvaluable)
                .map(EvaluacionCriterio::getPuntajeSugerido)
                .filter(p -> p != null)
                .toList();
        if (puntajes.isEmpty()) {
            return null;
        }
        BigDecimal suma = puntajes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return switch (modo) {
            case SUMA -> suma;
            case PROMEDIO -> suma.divide(BigDecimal.valueOf(puntajes.size()), 2, RoundingMode.HALF_UP);
        };
    }

    private static void anexar(StringBuilder sb, String texto) {
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(texto);
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.strip().toLowerCase().replaceAll("\\s+", " ");
    }
}
