package com.classroomiq.backend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.evaluacion.domain.EstadoEvaluacion;
import com.classroomiq.backend.evaluacion.domain.Evaluacion;
import com.classroomiq.backend.evaluacion.repository.EvaluacionCriterioRepository;
import com.classroomiq.backend.evaluacion.repository.EvaluacionRepository;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.provider.embeddings.EmbeddingProvider;
import com.classroomiq.backend.provider.llm.LlmProvider;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.LlmSolicitud;
import com.classroomiq.backend.provider.llm.ModeloTier;
import com.classroomiq.backend.provider.llm.UsoTokens;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Evaluación en background (Fase 4, Hito 4): tras indexar un lote ({@code /procesar}), dispararlo
 * con {@code /evaluar} genera el borrador en hilos del executor reusando la infra de Fase 3.
 * Verifica la propagación de tenant al hilo async (la evaluación queda bajo el tenant correcto e
 * invisible a otro) y que la entrega vuelve a LISTO con su borrador persistido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, EvaluacionBackgroundTest.Stubs.class})
class EvaluacionBackgroundTest {

    @TestConfiguration
    static class Stubs {
        @Bean
        EmbeddingProvider embeddingProvider() {
            return new EmbeddingProvider() {
                @Override
                public List<float[]> embed(List<String> textos) {
                    return textos.stream().map(t -> {
                        Random r = new Random(t.hashCode());
                        float[] v = new float[1024];
                        double s = 0;
                        for (int i = 0; i < v.length; i++) {
                            v[i] = r.nextFloat();
                            s += v[i] * v[i];
                        }
                        double n = Math.sqrt(s);
                        for (int i = 0; i < v.length; i++) {
                            v[i] /= n;
                        }
                        return v;
                    }).toList();
                }

                @Override
                public int dimension() {
                    return 1024;
                }

                @Override
                public String modelo() {
                    return "stub";
                }
            };
        }

        @Bean
        LlmProvider llmProvider() {
            return new LlmProvider() {
                @Override
                public LlmResultado generar(LlmSolicitud solicitud) {
                    String json = """
                            {"nivel":"Exc","puntaje":8,
                             "justificacion":"El trabajo implementa la suma.",
                             "citas":["def calcular"],"advertencia":null}
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
        registry.add("app.storage.base-path", () -> {
            try {
                return Files.createTempDirectory("classroomiq-eval-bg").toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        registry.add("app.embeddings.provider", () -> "stub");
        registry.add("app.llm.provider", () -> "fake");
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
    private EntregaRepository entregas;
    @Autowired
    private EvaluacionRepository evaluaciones;
    @Autowired
    private EvaluacionCriterioRepository evaluacionCriterios;

    private UUID tenantId;
    private String token;

    @BeforeEach
    void preparar() {
        TenantContext.clear();
        Institucion institucion = new Institucion();
        institucion.setNombre("Institución Eval BG");
        tenantId = instituciones.save(institucion).getId();
        token = login(nuevoDocente());
    }

    @Test
    void evaluaLoteEnBackgroundYPersisteBorradorConTenantCorrecto() throws Exception {
        UUID materiaId = crearMateria("Programación");
        UUID rubricaId = crearRubrica(materiaId);
        UUID loteId = crearLote(materiaId, rubricaId);
        UUID entregaId = subirCodigo(loteId, "g1");

        // 1) Indexar (Fase 3): la entrega queda LISTO.
        mvc.perform(post("/api/lotes/" + loteId + "/procesar").header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());
        esperarHasta(() -> estado(entregaId) == EstadoEntrega.LISTO);

        // 2) Evaluar (Fase 4): encola un job por la entrega indexada.
        mvc.perform(post("/api/lotes/" + loteId + "/evaluar").header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.entregasEncoladas").value(1));

        // La entrega vuelve a LISTO y existe el borrador.
        esperarHasta(() -> estado(entregaId) == EstadoEntrega.LISTO && existeEvaluacion(entregaId));

        TenantContext.set(tenantId);
        try {
            Evaluacion eval = evaluaciones.findByEntregaId(entregaId).orElseThrow();
            assertThat(eval.getEstado()).isEqualTo(EstadoEvaluacion.BORRADOR);
            assertThat(eval.getRubricaId()).isEqualTo(rubricaId);
            assertThat(eval.getPuntajeTotalSugerido()).isEqualByComparingTo("8.00");
            assertThat(evaluacionCriterios.findAllByEvaluacionIdOrderByOrdenAsc(eval.getId()))
                    .singleElement()
                    .satisfies(c -> {
                        assertThat(c.isEvaluable()).isTrue();
                        assertThat(c.getPuntajeSugerido()).isEqualByComparingTo("8.00");
                        assertThat(c.getNivelSugeridoId()).isNotNull();
                    });
        } finally {
            TenantContext.clear();
        }

        // Aislamiento: otro tenant no ve el borrador (la propagación de tenant al hilo async fue correcta).
        UUID otroTenant = nuevaInstitucion();
        TenantContext.set(otroTenant);
        try {
            assertThat(evaluaciones.existsByEntregaId(entregaId)).isFalse();
        } finally {
            TenantContext.clear();
        }
    }

    private boolean existeEvaluacion(UUID entregaId) {
        return enTenant(() -> evaluaciones.existsByEntregaId(entregaId));
    }

    private EstadoEntrega estado(UUID entregaId) {
        return enTenant(() -> entregas.findById(entregaId).orElseThrow().getEstado());
    }

    private <T> T enTenant(java.util.function.Supplier<T> accion) {
        boolean teniaContexto = TenantContext.get().isPresent();
        if (!teniaContexto) {
            TenantContext.set(tenantId);
        }
        try {
            return accion.get();
        } finally {
            if (!teniaContexto) {
                TenantContext.clear();
            }
        }
    }

    private void esperarHasta(java.util.function.BooleanSupplier condicion) throws InterruptedException {
        long limite = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("La evaluación no terminó dentro del tiempo límite");
    }

    // --- helpers de API ---

    private UUID subirCodigo(UUID loteId, String alias) throws Exception {
        MockMultipartFile zip = new MockMultipartFile("archivos", "proyecto.zip", "application/zip", zipCodigo());
        MvcResult res = mvc.perform(multipart("/api/lotes/" + loteId + "/entregas")
                        .file(zip)
                        .param("identificadorEstudiante", alias)
                        .param("tipo", "CODIGO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    private byte[] zipCodigo() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry("src/calculadora.py"));
            zip.write("def calcular(a, b):\n    return a + b\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return baos.toByteArray();
    }

    private UUID crearMateria(String nombre) throws Exception {
        MvcResult r = mvc.perform(post("/api/materias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID crearRubrica(UUID materiaId) throws Exception {
        String rubrica = """
                {"nombre":"R","puntajeTotal":10,"modoTotal":"SUMA","criterios":[
                  {"nombre":"C","puntajeMaximo":10,"niveles":[
                    {"nombre":"Exc","tipoPuntaje":"RANGO","puntajeMin":6,"puntajeMax":10},
                    {"nombre":"Ins","tipoPuntaje":"RANGO","puntajeMin":0,"puntajeMax":5}]}]}
                """;
        MvcResult r = mvc.perform(post("/api/materias/" + materiaId + "/rubricas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rubrica))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID crearLote(UUID materiaId, UUID rubricaId) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "materiaId", materiaId, "rubricaId", rubricaId, "nombre", "Lote Eval"));
        MvcResult r = mvc.perform(post("/api/lotes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
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

    private String nuevoDocente() {
        String email = "doc-" + UUID.randomUUID() + "@test.io";
        TenantContext.runWith(tenantId, () -> {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setNombre("Docente");
            u.setPasswordHash(passwordEncoder.encode("docente123"));
            u.setRol(Rol.DOCENTE);
            u.setActivo(true);
            usuarios.save(u);
        });
        return email;
    }

    private UUID nuevaInstitucion() {
        TenantContext.clear();
        Institucion i = new Institucion();
        i.setNombre("Otra " + UUID.randomUUID());
        return instituciones.save(i).getId();
    }
}
