package com.classroomiq.backend;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.classroomiq.backend.provider.llm.LlmProvider;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.LlmSolicitud;
import com.classroomiq.backend.provider.llm.ModeloTier;
import com.classroomiq.backend.provider.llm.UsoTokens;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.evaluacion.domain.EstadoEvaluacion;
import com.classroomiq.backend.evaluacion.domain.Evaluacion;
import com.classroomiq.backend.evaluacion.domain.EvaluacionCriterio;
import com.classroomiq.backend.evaluacion.repository.EvaluacionRepository;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.ModoTotal;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.domain.TipoPuntaje;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * API del resumen por grupo (Fase 5, Hito 4) vía MockMvc: estadísticas de notas, análisis por
 * criterio ordenado de mejor a peor, mapa de dominio (distribución por nivel), criterios difíciles,
 * la regla de "lote completo" (422 si falta aprobar) y el aislamiento por docente (404). Las
 * evaluaciones se siembran aprobadas con notas controladas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ResumenGrupoApiTest.Stubs.class})
class ResumenGrupoApiTest {

    static final String NARRATIVA_FAKE =
            "El grupo demostró dominio sólido en Correctitud, con dificultades sistemáticas en Estilo.";

    @TestConfiguration
    static class Stubs {
        /** LLM falso: ignora el prompt y devuelve un párrafo fijo (no llama a Anthropic). */
        @Bean
        LlmProvider llmProvider() {
            return new LlmProvider() {
                @Override
                public LlmResultado generar(LlmSolicitud solicitud) {
                    return new LlmResultado(NARRATIVA_FAKE, "fake", "end_turn", new UsoTokens(50, 30));
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
        registry.add("app.llm.provider", () -> "fake"); // excluye AnthropicLlmProvider
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private InstitucionRepository instituciones;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MateriaRepository materias;
    @Autowired
    private RubricaRepository rubricas;
    @Autowired
    private LoteRepository lotes;
    @Autowired
    private EntregaRepository entregas;
    @Autowired
    private EvaluacionRepository evaluaciones;

    private UUID tenantId;
    private UUID docenteId;
    private UUID materiaId;
    private String token;
    private UUID loteId;

    // ids de la rúbrica sembrada
    private UUID c1Id;
    private UUID c1ExcId;
    private UUID c1InsId;
    private UUID c2Id;
    private UUID c2ExcId;
    private UUID c2InsId;

    @BeforeEach
    void preparar() {
        TenantContext.clear();
        Institucion inst = new Institucion();
        inst.setNombre("Inst Resumen");
        tenantId = instituciones.save(inst).getId();
        String email = "doc-" + UUID.randomUUID() + "@res.test";
        docenteId = crearDocente(email);
        token = login(email);
    }

    @Test
    void resumeEstadisticasCriteriosYMapaDeDominio() throws Exception {
        seedLote(true);

        MvcResult res = mvc.perform(get("/api/lotes/" + loteId + "/resumen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvaluaciones").value(3))
                .andExpect(jsonPath("$.puntajeTotalRubrica").value(20.0))
                // Notas finales 18, 13, 11 -> promedio 14, mediana 13, min 11, max 18.
                .andExpect(jsonPath("$.estadisticas.promedio", closeTo(14.0, 0.01)))
                .andExpect(jsonPath("$.estadisticas.mediana", closeTo(13.0, 0.01)))
                .andExpect(jsonPath("$.estadisticas.minima", closeTo(11.0, 0.01)))
                .andExpect(jsonPath("$.estadisticas.maxima", closeTo(18.0, 0.01)))
                .andExpect(jsonPath("$.estadisticas.histograma.length()").value(5))
                // Mejor a peor: Correctitud (90%) antes que Estilo (50%).
                .andExpect(jsonPath("$.criterios[0].nombre").value("Correctitud"))
                .andExpect(jsonPath("$.criterios[0].promedioPct", closeTo(90.0, 0.01)))
                .andExpect(jsonPath("$.criterios[1].nombre").value("Estilo"))
                .andExpect(jsonPath("$.criterios[1].promedioPct", closeTo(50.0, 0.01)))
                // El criterio más difícil es Estilo.
                .andExpect(jsonPath("$.criteriosDificiles[0]").value("Estilo"))
                // Narrativa aún no generada (Hito 5).
                .andExpect(jsonPath("$.narrativa").doesNotExist())
                .andReturn();

        // Mapa de dominio de Correctitud: los 3 en Excelente (100%).
        var arbol = json.readTree(res.getResponse().getContentAsString());
        var distC1 = arbol.at("/criterios/0/distribucion");
        org.assertj.core.api.Assertions.assertThat(distC1.get(0).get("nombre").asText()).isEqualTo("Excelente");
        org.assertj.core.api.Assertions.assertThat(distC1.get(0).get("cantidad").asInt()).isEqualTo(3);
        // Estilo: 1 Excelente, 2 Insuficiente.
        var distC2 = arbol.at("/criterios/1/distribucion");
        org.assertj.core.api.Assertions.assertThat(distC2.get(0).get("cantidad").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(distC2.get(1).get("cantidad").asInt()).isEqualTo(2);
    }

    @Test
    void generaPersisteYDevuelveLaNarrativa() throws Exception {
        seedLote(true);

        // Antes de generar, el resumen no trae narrativa.
        mvc.perform(get("/api/lotes/" + loteId + "/resumen").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narrativa").doesNotExist());

        // POST genera la narrativa (LLM stub) y la devuelve incluida en el resumen.
        mvc.perform(post("/api/lotes/" + loteId + "/resumen/narrativa")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narrativa").value(NARRATIVA_FAKE))
                .andExpect(jsonPath("$.totalEvaluaciones").value(3));

        // GET posterior ya trae la narrativa persistida.
        mvc.perform(get("/api/lotes/" + loteId + "/resumen").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narrativa").value(NARRATIVA_FAKE));

        // Regenerar es idempotente (upsert in-place): sigue una sola narrativa.
        mvc.perform(post("/api/lotes/" + loteId + "/resumen/narrativa")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narrativa").value(NARRATIVA_FAKE));
    }

    @Test
    void noGeneraNarrativaSiElLoteNoEstaCompleto() throws Exception {
        seedLote(false);
        mvc.perform(post("/api/lotes/" + loteId + "/resumen/narrativa")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void exigeQueTodasLasEntregasEstenAprobadas() throws Exception {
        seedLote(false); // una evaluación queda en BORRADOR
        mvc.perform(get("/api/lotes/" + loteId + "/resumen").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void resumenDeOtroDocenteNoEsVisible() throws Exception {
        seedLote(true);
        String otroEmail = "otro-" + UUID.randomUUID() + "@res.test";
        crearDocente(otroEmail);
        String otroToken = login(otroEmail);
        mvc.perform(get("/api/lotes/" + loteId + "/resumen").header("Authorization", "Bearer " + otroToken))
                .andExpect(status().isNotFound());
    }

    // --- seed ---

    private void seedLote(boolean todasAprobadas) {
        TenantContext.set(tenantId);
        try {
            Materia materia = new Materia();
            materia.setDocenteId(docenteId);
            materia.setNombre("Programación");
            materiaId = materias.save(materia).getId();

            UUID rubricaId = crearRubrica();

            Lote lote = new Lote();
            lote.setDocenteId(docenteId);
            lote.setMateriaId(materiaId);
            lote.setRubricaId(rubricaId);
            lote.setNombre("Proyecto Final — Grupo A");
            loteId = lotes.save(lote).getId();

            // e1: C1=10(Exc) C2=8(Exc) total 18 ; e2: C1=9(Exc) C2=4(Ins) total 13 ;
            // e3: C1=8(Exc) C2=3(Ins) total 11. Promedios: C1=9 (90%), C2=5 (50%).
            crearEntregaConEval(rubricaId, "g1", "10.00", c1ExcId, "8.00", c2ExcId, "18.00", true);
            crearEntregaConEval(rubricaId, "g2", "9.00", c1ExcId, "4.00", c2InsId, "13.00", true);
            crearEntregaConEval(rubricaId, "g3", "8.00", c1ExcId, "3.00", c2InsId, "11.00", todasAprobadas);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID crearRubrica() {
        Rubrica rubrica = new Rubrica();
        rubrica.setDocenteId(docenteId);
        rubrica.setMateriaId(materiaId);
        rubrica.setNombre("Rúbrica");
        rubrica.setPuntajeTotal(new BigDecimal("20.00"));
        rubrica.setModoTotal(ModoTotal.SUMA);

        Criterio c1 = criterio("Correctitud", 0);
        c1.addNivel(nivel("Excelente", "6.00", "10.00", 0));
        c1.addNivel(nivel("Insuficiente", "0.00", "5.00", 1));
        Criterio c2 = criterio("Estilo", 1);
        c2.addNivel(nivel("Excelente", "6.00", "10.00", 0));
        c2.addNivel(nivel("Insuficiente", "0.00", "5.00", 1));
        rubrica.addCriterio(c1);
        rubrica.addCriterio(c2);
        rubrica = rubricas.save(rubrica);

        c1Id = rubrica.getCriterios().get(0).getId();
        c1ExcId = rubrica.getCriterios().get(0).getNiveles().get(0).getId();
        c1InsId = rubrica.getCriterios().get(0).getNiveles().get(1).getId();
        c2Id = rubrica.getCriterios().get(1).getId();
        c2ExcId = rubrica.getCriterios().get(1).getNiveles().get(0).getId();
        c2InsId = rubrica.getCriterios().get(1).getNiveles().get(1).getId();
        return rubrica.getId();
    }

    private void crearEntregaConEval(UUID rubricaId, String idEstud, String p1, UUID n1, String p2,
            UUID n2, String total, boolean aprobada) {
        Entrega entrega = new Entrega();
        entrega.setLoteId(loteId);
        entrega.setDocenteId(docenteId);
        entrega.setMateriaId(materiaId);
        entrega.setIdentificadorEstudiante(idEstud);
        entrega.setTipo(TipoEntrega.DOCUMENTO);
        entrega.setEstado(EstadoEntrega.LISTO);
        UUID entregaId = entregas.save(entrega).getId();

        Evaluacion eval = new Evaluacion();
        eval.setDocenteId(docenteId);
        eval.setEntregaId(entregaId);
        eval.setRubricaId(rubricaId);
        eval.setEstado(aprobada ? EstadoEvaluacion.APROBADA : EstadoEvaluacion.BORRADOR);
        eval.setPuntajeTotalSugerido(new BigDecimal(total));
        if (aprobada) {
            eval.setPuntajeTotalFinal(new BigDecimal(total));
        }
        eval.addCriterio(criterioEval(c1Id, n1, p1, 0));
        eval.addCriterio(criterioEval(c2Id, n2, p2, 1));
        evaluaciones.save(eval);
    }

    private EvaluacionCriterio criterioEval(UUID criterioId, UUID nivelId, String puntaje, int orden) {
        EvaluacionCriterio ec = new EvaluacionCriterio();
        ec.setCriterioId(criterioId);
        ec.setEvaluable(true);
        ec.setNivelSugeridoId(nivelId);
        ec.setPuntajeSugerido(new BigDecimal(puntaje));
        ec.setNivelFinalId(nivelId);
        ec.setPuntajeFinal(new BigDecimal(puntaje));
        ec.setOrden(orden);
        return ec;
    }

    private Criterio criterio(String nombre, int orden) {
        Criterio c = new Criterio();
        c.setNombre(nombre);
        c.setPuntajeMaximo(new BigDecimal("10.00"));
        c.setOrden(orden);
        return c;
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

    private UUID crearDocente(String email) {
        TenantContext.set(tenantId);
        try {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setNombre("Docente");
            u.setPasswordHash(passwordEncoder.encode("docente123"));
            u.setRol(Rol.DOCENTE);
            u.setActivo(true);
            return usuarios.save(u).getId();
        } finally {
            TenantContext.clear();
        }
    }

    private String login(String email) {
        try {
            MvcResult r = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "password", "docente123"))))
                    .andExpect(status().isOk())
                    .andReturn();
            return json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
