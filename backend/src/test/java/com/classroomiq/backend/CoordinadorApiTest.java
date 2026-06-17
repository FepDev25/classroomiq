package com.classroomiq.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
 * Acceso del coordinador (Fase 5, Hito 6) vía MockMvc: el admin asigna materias, el coordinador ve
 * el resumen agregado de sus materias asignadas y NADA de lo no asignado (404), no puede usar
 * endpoints de docente (403) ni el docente los de coordinador (403), y solo se asignan materias a
 * usuarios COORDINADOR (422).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CoordinadorApiTest {

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
    private UUID coordinadorId;
    private String adminToken;
    private String docenteToken;
    private String coordToken;

    private UUID materiaId;
    private UUID loteId;

    @BeforeEach
    void preparar() {
        TenantContext.clear();
        Institucion inst = new Institucion();
        inst.setNombre("Inst Coord");
        tenantId = instituciones.save(inst).getId();

        String adminEmail = "admin-" + UUID.randomUUID() + "@co.test";
        String docEmail = "doc-" + UUID.randomUUID() + "@co.test";
        String coordEmail = "coord-" + UUID.randomUUID() + "@co.test";
        crearUsuario(adminEmail, Rol.ADMIN);
        docenteId = crearUsuario(docEmail, Rol.DOCENTE);
        coordinadorId = crearUsuario(coordEmail, Rol.COORDINADOR);
        adminToken = login(adminEmail);
        docenteToken = login(docEmail);
        coordToken = login(coordEmail);

        seedLoteAprobado();
    }

    @Test
    void coordinadorVeResumenDeMateriaAsignada() throws Exception {
        // Antes de asignar: el coordinador no ve la materia ni su resumen.
        mvc.perform(get("/api/coordinador/materias").header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/coordinador/lotes/" + loteId + "/resumen")
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isNotFound());

        // El admin asigna la materia al coordinador.
        mvc.perform(post("/api/admin/coordinadores/" + coordinadorId + "/materias/" + materiaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(materiaId.toString()));

        // Ahora el coordinador ve la materia, su lote y el resumen agregado (estadísticas).
        mvc.perform(get("/api/coordinador/materias").header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/coordinador/materias/" + materiaId + "/lotes")
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(loteId.toString()));
        mvc.perform(get("/api/coordinador/lotes/" + loteId + "/resumen")
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvaluaciones").value(2))
                .andExpect(jsonPath("$.criterios[0].nombre").value("Correctitud"));

        // Al desasignar, vuelve a perder el acceso.
        mvc.perform(delete("/api/admin/coordinadores/" + coordinadorId + "/materias/" + materiaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/coordinador/lotes/" + loteId + "/resumen")
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void coordinadorNoAccedeEndpointsDeDocente() throws Exception {
        // Endpoints de docente (similitud / evaluación) rechazan el rol COORDINADOR.
        mvc.perform(post("/api/lotes/" + loteId + "/similitud").header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/lotes/" + loteId + "/resumen").header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void docenteNoAccedeEndpointsDeCoordinador() throws Exception {
        mvc.perform(get("/api/coordinador/materias").header("Authorization", "Bearer " + docenteToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/coordinador/lotes/" + loteId + "/resumen")
                        .header("Authorization", "Bearer " + docenteToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloSeAsignanMateriasAUnCoordinador() throws Exception {
        // Asignar a un usuario DOCENTE -> 422 (no es coordinador).
        mvc.perform(post("/api/admin/coordinadores/" + docenteId + "/materias/" + materiaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- seed ---

    private void seedLoteAprobado() {
        TenantContext.set(tenantId);
        try {
            Materia materia = new Materia();
            materia.setDocenteId(docenteId);
            materia.setNombre("Programación");
            materiaId = materias.save(materia).getId();

            Rubrica rubrica = new Rubrica();
            rubrica.setDocenteId(docenteId);
            rubrica.setMateriaId(materiaId);
            rubrica.setNombre("Rúbrica");
            rubrica.setPuntajeTotal(new BigDecimal("10.00"));
            rubrica.setModoTotal(ModoTotal.SUMA);
            Criterio c = new Criterio();
            c.setNombre("Correctitud");
            c.setPuntajeMaximo(new BigDecimal("10.00"));
            c.setOrden(0);
            NivelDesempeno exc = nivel("Excelente", "6.00", "10.00", 0);
            c.addNivel(exc);
            c.addNivel(nivel("Insuficiente", "0.00", "5.00", 1));
            rubrica.addCriterio(c);
            rubrica = rubricas.save(rubrica);
            UUID rubricaId = rubrica.getId();
            UUID criterioId = rubrica.getCriterios().get(0).getId();
            UUID nivelId = rubrica.getCriterios().get(0).getNiveles().get(0).getId();

            Lote lote = new Lote();
            lote.setDocenteId(docenteId);
            lote.setMateriaId(materiaId);
            lote.setRubricaId(rubricaId);
            lote.setNombre("Proyecto Final");
            loteId = lotes.save(lote).getId();

            crearEntregaAprobada(rubricaId, criterioId, nivelId, "g1", "9.00");
            crearEntregaAprobada(rubricaId, criterioId, nivelId, "g2", "7.00");
        } finally {
            TenantContext.clear();
        }
    }

    private void crearEntregaAprobada(UUID rubricaId, UUID criterioId, UUID nivelId, String id,
            String puntaje) {
        Entrega entrega = new Entrega();
        entrega.setLoteId(loteId);
        entrega.setDocenteId(docenteId);
        entrega.setMateriaId(materiaId);
        entrega.setIdentificadorEstudiante(id);
        entrega.setTipo(TipoEntrega.DOCUMENTO);
        entrega.setEstado(EstadoEntrega.LISTO);
        UUID entregaId = entregas.save(entrega).getId();

        Evaluacion eval = new Evaluacion();
        eval.setDocenteId(docenteId);
        eval.setEntregaId(entregaId);
        eval.setRubricaId(rubricaId);
        eval.setEstado(EstadoEvaluacion.APROBADA);
        eval.setPuntajeTotalSugerido(new BigDecimal(puntaje));
        eval.setPuntajeTotalFinal(new BigDecimal(puntaje));
        EvaluacionCriterio ec = new EvaluacionCriterio();
        ec.setCriterioId(criterioId);
        ec.setEvaluable(true);
        ec.setNivelSugeridoId(nivelId);
        ec.setPuntajeSugerido(new BigDecimal(puntaje));
        ec.setNivelFinalId(nivelId);
        ec.setPuntajeFinal(new BigDecimal(puntaje));
        ec.setOrden(0);
        eval.addCriterio(ec);
        evaluaciones.save(eval);
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

    private UUID crearUsuario(String email, Rol rol) {
        TenantContext.set(tenantId);
        try {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setNombre("Usuario");
            u.setPasswordHash(passwordEncoder.encode("clave123"));
            u.setRol(rol);
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
                            .content(json.writeValueAsString(Map.of("email", email, "password", "clave123"))))
                    .andExpect(status().isOk())
                    .andReturn();
            return json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
