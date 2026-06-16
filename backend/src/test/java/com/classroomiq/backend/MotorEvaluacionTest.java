package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.FragmentoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.evaluacion.domain.CitaFragmento;
import com.classroomiq.backend.evaluacion.domain.EstadoEvaluacion;
import com.classroomiq.backend.evaluacion.domain.Evaluacion;
import com.classroomiq.backend.evaluacion.domain.EvaluacionCriterio;
import com.classroomiq.backend.evaluacion.motor.MotorEvaluacion;
import com.classroomiq.backend.evaluacion.repository.CitaFragmentoRepository;
import com.classroomiq.backend.evaluacion.repository.EvaluacionCriterioRepository;
import com.classroomiq.backend.evaluacion.repository.EvaluacionRepository;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.provider.embeddings.EmbeddingProvider;
import com.classroomiq.backend.provider.llm.LlmProvider;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.LlmSolicitud;
import com.classroomiq.backend.provider.llm.ModeloTier;
import com.classroomiq.backend.provider.llm.UsoTokens;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.ModoTotal;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.domain.TipoPuntaje;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Motor de evaluación (Fase 4, Hito 3) contra un Postgres real: genera el borrador criterio por
 * criterio con un {@link LlmProvider} y un {@link EmbeddingProvider} stub (no depende de Anthropic
 * ni de Ollama). Verifica: nivel emparejado por nombre, puntaje fuera de rango acotado con
 * advertencia, criterio no-evaluable dejado en blanco para el docente, cita emparejada al fragmento
 * de origen, total proyectado según ModoTotal, y reevaluación idempotente.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MotorEvaluacionTest.Stubs.class})
class MotorEvaluacionTest {

    @TestConfiguration
    static class Stubs {
        @Bean
        EmbeddingProvider embeddingProvider() {
            return new EmbeddingProvider() {
                @Override
                public List<float[]> embed(List<String> textos) {
                    return textos.stream().map(this::vector).toList();
                }

                @Override
                public int dimension() {
                    return 1024;
                }

                @Override
                public String modelo() {
                    return "stub";
                }

                private float[] vector(String texto) {
                    float[] v = new float[1024];
                    int h = Math.abs(texto.hashCode());
                    v[h % 1024] = 1.0f; // vector unitario determinista
                    return v;
                }
            };
        }

        /** LLM falso: ignora el prompt y devuelve un JSON envuelto en prosa + cerco markdown. */
        @Bean
        LlmProvider llmProvider() {
            return new LlmProvider() {
                @Override
                public LlmResultado generar(LlmSolicitud solicitud) {
                    String json = """
                            Aquí está mi evaluación del criterio:
                            ```json
                            {
                              "nivel": "excelente",
                              "puntaje": 25,
                              "justificacion": "El trabajo implementa la operación de suma.",
                              "citas": ["def calcular"],
                              "advertencia": null
                            }
                            ```
                            """;
                    return new LlmResultado(json, "fake", "end_turn", new UsoTokens(10, 20));
                }

                @Override
                public String modelo(ModeloTier tier) {
                    return "fake";
                }
            };
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.embeddings.provider", () -> "stub");
        registry.add("app.llm.provider", () -> "fake"); // excluye AnthropicLlmProvider
    }

    @Autowired
    private InstitucionRepository instituciones;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private MateriaRepository materias;
    @Autowired
    private RubricaRepository rubricas;
    @Autowired
    private LoteRepository lotes;
    @Autowired
    private EntregaRepository entregas;
    @Autowired
    private FragmentoEntregaRepository fragmentos;
    @Autowired
    private EvaluacionRepository evaluaciones;
    @Autowired
    private EvaluacionCriterioRepository evaluacionCriterios;
    @Autowired
    private CitaFragmentoRepository citas;
    @Autowired
    private MotorEvaluacion motor;

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    @Test
    void generaBorradorPorCriterioConClampCitaYNoEvaluableEnBlanco() {
        UUID tenant = nuevaInstitucion("Inst Motor");
        UUID docente = nuevoDocente(tenant, "doc@motor.test");
        UUID materia = nuevaMateria(tenant, docente);
        Rubrica rubrica = nuevaRubrica(tenant, docente, materia);
        UUID criterioEvaluable = rubrica.getCriterios().get(0).getId();
        UUID criterioNoEvaluable = rubrica.getCriterios().get(1).getId();

        TenantContext.set(tenant);
        try {
            UUID entregaId = nuevaEntregaLista(docente, materia, rubrica.getId());
            UUID fragId = nuevoFragmento(docente, materia, entregaId,
                    "def calcular(a, b):\n    return a + b");

            Evaluacion eval = motor.evaluar(entregaId);

            assertThat(eval.getEstado()).isEqualTo(EstadoEvaluacion.BORRADOR);
            assertThat(eval.getRubricaId()).isEqualTo(rubrica.getId());
            assertThat(evaluaciones.existsByEntregaId(entregaId)).isTrue();

            List<EvaluacionCriterio> crits =
                    evaluacionCriterios.findAllByEvaluacionIdOrderByOrdenAsc(eval.getId());
            assertThat(crits).hasSize(2);

            EvaluacionCriterio evaluado = crits.stream()
                    .filter(c -> c.getCriterioId().equals(criterioEvaluable)).findFirst().orElseThrow();
            assertThat(evaluado.isEvaluable()).isTrue();
            assertThat(evaluado.getNivelSugeridoId()).isNotNull();
            assertThat(evaluado.getJustificacion()).contains("suma");
            // El LLM propuso 25, fuera del rango 17–20: se acota a 20 y se advierte.
            assertThat(evaluado.getPuntajeSugerido()).isEqualByComparingTo("20.00");
            assertThat(evaluado.getAdvertencia()).contains("fuera del rango");

            // La cita "def calcular" se emparejó con el fragmento de origen.
            List<CitaFragmento> citasCrit =
                    citas.findAllByEvaluacionCriterioIdOrderByOrdenAsc(evaluado.getId());
            assertThat(citasCrit).singleElement().satisfies(c -> {
                assertThat(c.getTextoCitado()).isEqualTo("def calcular");
                assertThat(c.getFragmentoId()).isEqualTo(fragId);
            });

            // El criterio no evaluable por contenido queda en blanco, marcado para el docente.
            EvaluacionCriterio enBlanco = crits.stream()
                    .filter(c -> c.getCriterioId().equals(criterioNoEvaluable)).findFirst().orElseThrow();
            assertThat(enBlanco.isEvaluable()).isFalse();
            assertThat(enBlanco.getPuntajeSugerido()).isNull();
            assertThat(enBlanco.getNivelSugeridoId()).isNull();
            assertThat(enBlanco.getAdvertencia()).contains("juicio del docente");

            // Total proyectado (ModoTotal.SUMA) solo sobre los criterios con puntaje: 20.
            assertThat(eval.getPuntajeTotalSugerido()).isEqualByComparingTo("20.00");

            // Reevaluación idempotente: sigue habiendo una sola evaluación y dos criterios.
            Evaluacion reeval = motor.evaluar(entregaId);
            assertThat(reeval.getId()).isNotEqualTo(eval.getId());
            assertThat(evaluacionCriterios.findAllByEvaluacionIdOrderByOrdenAsc(reeval.getId())).hasSize(2);
            assertThat(evaluacionCriterios.findAllByEvaluacionIdOrderByOrdenAsc(eval.getId())).isEmpty();
        } finally {
            TenantContext.clear();
        }
    }

    // --- helpers ---

    private UUID nuevaEntregaLista(UUID docente, UUID materia, UUID rubricaId) {
        Lote lote = new Lote();
        lote.setDocenteId(docente);
        lote.setMateriaId(materia);
        lote.setRubricaId(rubricaId);
        lote.setNombre("Lote");
        UUID loteId = lotes.save(lote).getId();

        Entrega entrega = new Entrega();
        entrega.setLoteId(loteId);
        entrega.setDocenteId(docente);
        entrega.setMateriaId(materia);
        entrega.setIdentificadorEstudiante("g1");
        entrega.setTipo(TipoEntrega.CODIGO);
        entrega.setEstado(EstadoEntrega.LISTO);
        return entregas.save(entrega).getId();
    }

    private UUID nuevoFragmento(UUID docente, UUID materia, UUID entregaId, String contenido) {
        Entrega entrega = entregas.findById(entregaId).orElseThrow();
        float[] emb = new float[1024];
        emb[0] = 1.0f;
        FragmentoEntrega frag = new FragmentoEntrega();
        frag.setDocenteId(docente);
        frag.setMateriaId(materia);
        frag.setLoteId(entrega.getLoteId());
        frag.setEntregaId(entregaId);
        frag.setOrden(0);
        frag.setContenido(contenido);
        frag.setSeccion("calculadora.py");
        frag.setEmbedding(emb);
        return fragmentos.save(frag).getId();
    }

    private UUID nuevaInstitucion(String nombre) {
        TenantContext.clear();
        Institucion i = new Institucion();
        i.setNombre(nombre);
        return instituciones.save(i).getId();
    }

    private UUID nuevoDocente(UUID tenant, String email) {
        TenantContext.set(tenant);
        try {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setPasswordHash("x");
            u.setNombre("Docente");
            u.setRol(Rol.DOCENTE);
            return usuarios.save(u).getId();
        } finally {
            TenantContext.clear();
        }
    }

    private UUID nuevaMateria(UUID tenant, UUID docente) {
        TenantContext.set(tenant);
        try {
            Materia m = new Materia();
            m.setDocenteId(docente);
            m.setNombre("Programación");
            return materias.save(m).getId();
        } finally {
            TenantContext.clear();
        }
    }

    /** Rúbrica con un criterio evaluable por contenido y uno que requiere juicio del docente. */
    private Rubrica nuevaRubrica(UUID tenant, UUID docente, UUID materia) {
        TenantContext.set(tenant);
        try {
            Rubrica r = new Rubrica();
            r.setDocenteId(docente);
            r.setMateriaId(materia);
            r.setNombre("Rúbrica");
            r.setPuntajeTotal(new BigDecimal("40.00"));
            r.setModoTotal(ModoTotal.SUMA);

            Criterio correctitud = new Criterio();
            correctitud.setNombre("Correctitud");
            correctitud.setPuntajeMaximo(new BigDecimal("20.00"));
            correctitud.setEvaluablePorContenido(true);
            correctitud.setOrden(0);
            correctitud.addNivel(nivel("Excelente", "17.00", "20.00", 0));
            correctitud.addNivel(nivel("Insuficiente", "0.00", "7.00", 1));
            r.addCriterio(correctitud);

            Criterio exposicion = new Criterio();
            exposicion.setNombre("Exposición oral");
            exposicion.setPuntajeMaximo(new BigDecimal("20.00"));
            exposicion.setEvaluablePorContenido(false);
            exposicion.setOrden(1);
            exposicion.addNivel(nivel("Excelente", "17.00", "20.00", 0));
            exposicion.addNivel(nivel("Insuficiente", "0.00", "7.00", 1));
            r.addCriterio(exposicion);

            return rubricas.save(r);
        } finally {
            TenantContext.clear();
        }
    }

    private NivelDesempeno nivel(String nombre, String min, String max, int orden) {
        NivelDesempeno n = new NivelDesempeno();
        n.setNombre(nombre);
        n.setTipoPuntaje(TipoPuntaje.RANGO);
        n.setPuntajeMin(new BigDecimal(min));
        n.setPuntajeMax(new BigDecimal(max));
        n.setOrden(orden);
        return n;
    }
}
