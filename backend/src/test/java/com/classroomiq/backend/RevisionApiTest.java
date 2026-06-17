package com.classroomiq.backend;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * API de revisión del borrador (Fase 4, Hito 5) vía MockMvc: leer el borrador enriquecido con la
 * rúbrica, editar un criterio (con validación de rango), fijar el comentario, aprobar (recalcula y
 * congela), el rechazo a editar tras aprobar, y el aislamiento por docente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RevisionApiTest {

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
    private FragmentoEntregaRepository fragmentos;
    @Autowired
    private EvaluacionRepository evaluaciones;

    private UUID tenantId;
    private UUID docenteId;
    private String token;

    private UUID entregaId;
    private UUID evaluacionId;
    private UUID criterioEvalId;
    private UUID nivelExcId;

    @BeforeEach
    void preparar() {
        TenantContext.clear();
        Institucion inst = new Institucion();
        inst.setNombre("Inst Revisión");
        tenantId = instituciones.save(inst).getId();
        String email = "doc-" + UUID.randomUUID() + "@rev.test";
        docenteId = crearDocente(email);
        token = login(email);
        seedBorrador();
    }

    @Test
    void leeEditaYApruebaElBorrador() throws Exception {
        // GET: borrador enriquecido con la rúbrica (nombre, niveles, citas).
        mvc.perform(get("/api/entregas/" + entregaId + "/evaluacion").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.criterios[0].nombreCriterio").value("Correctitud"))
                .andExpect(jsonPath("$.criterios[0].puntajeSugerido").value(8))
                .andExpect(jsonPath("$.criterios[0].niveles.length()").value(2))
                .andExpect(jsonPath("$.criterios[0].citas[0].textoCitado").value("def calcular"));

        // PATCH criterio: el docente fija nivel y puntaje y marca revisado.
        String edicion = json.writeValueAsString(Map.of(
                "nivelFinalId", nivelExcId, "puntajeFinal", 9, "justificacionEditada", "Correcto.",
                "revisadoManual", true));
        mvc.perform(patch("/api/evaluaciones/" + evaluacionId + "/criterios/" + criterioEvalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(edicion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntajeFinal").value(9))
                .andExpect(jsonPath("$.revisadoManual").value(true));

        // PATCH comentario general.
        mvc.perform(patch("/api/evaluaciones/" + evaluacionId + "/comentario")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comentarioGeneral\":\"Buen trabajo.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comentarioGeneral").value("Buen trabajo."));

        // POST aprobar: recalcula el total final (SUMA = 9) y congela.
        mvc.perform(post("/api/evaluaciones/" + evaluacionId + "/aprobar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.puntajeTotalFinal").value(9));

        // Tras aprobar, no se puede editar (409).
        mvc.perform(patch("/api/evaluaciones/" + evaluacionId + "/criterios/" + criterioEvalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(edicion))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazaPuntajeFueraDeRango() throws Exception {
        String fuera = json.writeValueAsString(Map.of(
                "nivelFinalId", nivelExcId, "puntajeFinal", 15, "revisadoManual", false));
        mvc.perform(patch("/api/evaluaciones/" + evaluacionId + "/criterios/" + criterioEvalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(fuera))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void elBorradorDeOtroDocenteNoEsVisible() throws Exception {
        String otroEmail = "otro-" + UUID.randomUUID() + "@rev.test";
        crearDocente(otroEmail);
        String otroToken = login(otroEmail);
        mvc.perform(get("/api/entregas/" + entregaId + "/evaluacion").header("Authorization", "Bearer " + otroToken))
                .andExpect(status().isNotFound());
    }

    // --- seed ---

    private void seedBorrador() {
        TenantContext.set(tenantId);
        try {
            Materia materia = new Materia();
            materia.setDocenteId(docenteId);
            materia.setNombre("Programación");
            UUID materiaId = materias.save(materia).getId();

            Rubrica rubrica = new Rubrica();
            rubrica.setDocenteId(docenteId);
            rubrica.setMateriaId(materiaId);
            rubrica.setNombre("Rúbrica");
            rubrica.setPuntajeTotal(new BigDecimal("10.00"));
            rubrica.setModoTotal(ModoTotal.SUMA);
            Criterio criterio = new Criterio();
            criterio.setNombre("Correctitud");
            criterio.setPuntajeMaximo(new BigDecimal("10.00"));
            criterio.setOrden(0);
            NivelDesempeno exc = nivel("Excelente", "6.00", "10.00", 0);
            criterio.addNivel(exc);
            criterio.addNivel(nivel("Insuficiente", "0.00", "5.00", 1));
            rubrica.addCriterio(criterio);
            rubrica = rubricas.save(rubrica);
            UUID rubricaId = rubrica.getId();
            UUID criterioId = rubrica.getCriterios().get(0).getId();
            nivelExcId = rubrica.getCriterios().get(0).getNiveles().get(0).getId();

            Lote lote = new Lote();
            lote.setDocenteId(docenteId);
            lote.setMateriaId(materiaId);
            lote.setRubricaId(rubricaId);
            lote.setNombre("Lote");
            UUID loteId = lotes.save(lote).getId();

            Entrega entrega = new Entrega();
            entrega.setLoteId(loteId);
            entrega.setDocenteId(docenteId);
            entrega.setMateriaId(materiaId);
            entrega.setIdentificadorEstudiante("g1");
            entrega.setTipo(TipoEntrega.CODIGO);
            entrega.setEstado(EstadoEntrega.LISTO);
            entregaId = entregas.save(entrega).getId();

            FragmentoEntrega frag = new FragmentoEntrega();
            frag.setDocenteId(docenteId);
            frag.setMateriaId(materiaId);
            frag.setLoteId(loteId);
            frag.setEntregaId(entregaId);
            frag.setOrden(0);
            frag.setContenido("def calcular(a, b): return a + b");
            float[] emb = new float[1024];
            emb[0] = 1.0f;
            frag.setEmbedding(emb);
            UUID fragId = fragmentos.save(frag).getId();

            Evaluacion eval = new Evaluacion();
            eval.setDocenteId(docenteId);
            eval.setEntregaId(entregaId);
            eval.setRubricaId(rubricaId);
            eval.setPuntajeTotalSugerido(new BigDecimal("8.00"));
            EvaluacionCriterio ec = new EvaluacionCriterio();
            ec.setCriterioId(criterioId);
            ec.setEvaluable(true);
            ec.setNivelSugeridoId(nivelExcId);
            ec.setPuntajeSugerido(new BigDecimal("8.00"));
            ec.setJustificacion("Implementa la suma.");
            ec.setOrden(0);
            CitaFragmento cita = new CitaFragmento();
            cita.setFragmentoId(fragId);
            cita.setTextoCitado("def calcular");
            cita.setOrden(0);
            ec.addCita(cita);
            eval.addCriterio(ec);
            eval = evaluaciones.save(eval);
            evaluacionId = eval.getId();
            criterioEvalId = eval.getCriterios().get(0).getId();
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
