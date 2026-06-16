package com.classroomiq.backend;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
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
import com.classroomiq.backend.entrega.domain.FragmentoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;
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
 * API de similitud (Fase 5, Hito 3) vía MockMvc: generar el reporte de un lote, el orden de pares
 * por similitud semántica, el marcado por umbral, los fragmentos lado a lado, el aislamiento por
 * docente (404) y la regla de mínimo 2 entregas (422). Los embeddings y textos se fabrican a mano
 * para forzar un par casi idéntico (copia) y uno distinto, sin depender de Ollama.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SimilitudApiTest {

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

    private static final String COPIA =
            "la rapida zorra parda salta sobre el perro perezoso cada sola manana de invierno";
    private static final String DISTINTO =
            "implementacion alternativa con arboles binarios de busqueda balanceados y recursion";

    private UUID tenantId;
    private UUID docenteId;
    private UUID materiaId;
    private String token;
    private UUID loteId;

    @BeforeEach
    void preparar() {
        TenantContext.clear();
        Institucion inst = new Institucion();
        inst.setNombre("Inst Similitud");
        tenantId = instituciones.save(inst).getId();
        String email = "doc-" + UUID.randomUUID() + "@sim.test";
        docenteId = crearDocente(email);
        token = login(email);
    }

    @Test
    void generaReporteOrdenadoConParCopiaMarcado() throws Exception {
        seedLoteConTresEntregas(true);

        // POST genera (umbral por defecto 0.75). 3 entregas -> C(3,2) = 3 pares.
        MvcResult res = mvc.perform(post("/api/lotes/" + loteId + "/similitud")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aviso", notNullValue()))
                .andExpect(jsonPath("$.entregas.length()").value(3))
                .andExpect(jsonPath("$.pares.length()").value(3))
                // El primer par (orden desc) es la copia: semántica ≈ 1, textual ≈ 1, marcado.
                .andExpect(jsonPath("$.pares[0].similitudSemantica", closeTo(1.0, 0.01)))
                .andExpect(jsonPath("$.pares[0].similitudTextual", closeTo(1.0, 0.01)))
                .andExpect(jsonPath("$.pares[0].superaUmbral").value(true))
                .andExpect(jsonPath("$.pares[0].fragmentos.length()", greaterThanOrEqualTo(1)))
                // El último par no supera el umbral (entrega distinta, ortogonal).
                .andExpect(jsonPath("$.pares[2].superaUmbral").value(false))
                .andExpect(jsonPath("$.pares[2].similitudSemantica", closeTo(0.0, 0.01)))
                .andReturn();

        // GET devuelve el reporte persistido (mismos pares).
        mvc.perform(get("/api/lotes/" + loteId + "/similitud").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pares.length()").value(3))
                .andExpect(jsonPath("$.pares[0].superaUmbral").value(true));

        // Regenerar es idempotente: sigue habiendo 3 pares (no se duplican).
        mvc.perform(post("/api/lotes/" + loteId + "/similitud").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pares.length()").value(3));

        assertReporteValido(res);
    }

    @Test
    void reporteDeOtroDocenteNoEsVisible() throws Exception {
        seedLoteConTresEntregas(true);
        mvc.perform(post("/api/lotes/" + loteId + "/similitud").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String otroEmail = "otro-" + UUID.randomUUID() + "@sim.test";
        crearDocente(otroEmail);
        String otroToken = login(otroEmail);
        mvc.perform(get("/api/lotes/" + loteId + "/similitud").header("Authorization", "Bearer " + otroToken))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/lotes/" + loteId + "/similitud").header("Authorization", "Bearer " + otroToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void exigeAlMenosDosEntregasListas() throws Exception {
        seedLoteConTresEntregas(false); // solo 1 entrega LISTO, las otras PENDIENTE
        mvc.perform(post("/api/lotes/" + loteId + "/similitud").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void leeAntesDeGenerarDevuelve404() throws Exception {
        seedLoteConTresEntregas(true);
        mvc.perform(get("/api/lotes/" + loteId + "/similitud").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private void assertReporteValido(MvcResult res) throws Exception {
        var arbol = json.readTree(res.getResponse().getContentAsString());
        double s0 = arbol.at("/pares/0/similitudSemantica").asDouble();
        double s2 = arbol.at("/pares/2/similitudSemantica").asDouble();
        org.assertj.core.api.Assertions.assertThat(s0).isGreaterThanOrEqualTo(s2);
    }

    // --- seed ---

    private void seedLoteConTresEntregas(boolean todasListas) {
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
            lote.setRubricaId(rubricaId); // la similitud no la usa, pero el lote la referencia por FK
            lote.setNombre("Lote A");
            loteId = lotes.save(lote).getId();

            // Dos copias (mismo texto y mismo embedding) y una distinta (texto + embedding ortogonal).
            crearEntrega("g1", COPIA, indice(0), EstadoEntrega.LISTO);
            crearEntrega("g2", COPIA, indice(0), todasListas ? EstadoEntrega.LISTO : EstadoEntrega.PENDIENTE);
            crearEntrega("g3", DISTINTO, indice(1), todasListas ? EstadoEntrega.LISTO : EstadoEntrega.PENDIENTE);
        } finally {
            TenantContext.clear();
        }
    }

    /** Rúbrica mínima válida solo para satisfacer la FK del lote (la similitud no la consulta). */
    private UUID crearRubrica() {
        Rubrica rubrica = new Rubrica();
        rubrica.setDocenteId(docenteId);
        rubrica.setMateriaId(materiaId);
        rubrica.setNombre("Rúbrica");
        rubrica.setPuntajeTotal(new BigDecimal("10.00"));
        rubrica.setModoTotal(ModoTotal.SUMA);
        Criterio criterio = new Criterio();
        criterio.setNombre("Criterio");
        criterio.setPuntajeMaximo(new BigDecimal("10.00"));
        criterio.setOrden(0);
        NivelDesempeno nivel = new NivelDesempeno();
        nivel.setNombre("Excelente");
        nivel.setTipoPuntaje(TipoPuntaje.RANGO);
        nivel.setPuntajeMin(new BigDecimal("6.00"));
        nivel.setPuntajeMax(new BigDecimal("10.00"));
        nivel.setOrden(0);
        criterio.addNivel(nivel);
        rubrica.addCriterio(criterio);
        return rubricas.save(rubrica).getId();
    }

    private void crearEntrega(String id, String contenido, float[] embedding, EstadoEntrega estado) {
        Entrega entrega = new Entrega();
        entrega.setLoteId(loteId);
        entrega.setDocenteId(docenteId);
        entrega.setMateriaId(materiaId);
        entrega.setIdentificadorEstudiante(id);
        entrega.setTipo(TipoEntrega.DOCUMENTO);
        entrega.setEstado(estado);
        UUID entregaId = entregas.save(entrega).getId();

        FragmentoEntrega frag = new FragmentoEntrega();
        frag.setDocenteId(docenteId);
        frag.setMateriaId(materiaId);
        frag.setLoteId(loteId);
        frag.setEntregaId(entregaId);
        frag.setOrden(0);
        frag.setContenido(contenido);
        frag.setEmbedding(embedding);
        fragmentos.save(frag);
    }

    /** Vector unitario 1024-dim con un 1.0 en la posición dada (vectores ortogonales entre índices). */
    private float[] indice(int i) {
        float[] v = new float[1024];
        v[i] = 1.0f;
        return v;
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
