package com.classroomiq.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.metricas.domain.OperacionLlm;
import com.classroomiq.backend.metricas.domain.RegistroUsoLlm;
import com.classroomiq.backend.metricas.repository.RegistroUsoLlmRepository;
import com.classroomiq.backend.provider.llm.ModeloTier;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Métricas de uso/costo del admin (Fase 6) vía MockMvc: agregación por docente con costo estimado y
 * total, bandera de umbral superado, desglose por modelo/operación, aislamiento por tenant (el admin
 * de una institución no ve el uso de otra) y autorización (solo ADMIN; docente -> 403).
 *
 * <p>El umbral se baja a 5.00 por configuración para poder verificar la bandera con datos chicos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MetricasApiTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.costos.umbral-mensual", () -> "5.00");
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
    private RegistroUsoLlmRepository registros;

    private UUID tenantA;
    private UUID docenteA1;
    private UUID docenteA2;
    private String adminAToken;
    private String docenteAToken;

    private UUID tenantB;
    private String adminBToken;

    @BeforeEach
    void preparar() {
        TenantContext.clear();
        tenantA = nuevaInstitucion("Inst A");
        String adminAEmail = "adminA-" + UUID.randomUUID() + "@m.test";
        String docA1Email = "docA1-" + UUID.randomUUID() + "@m.test";
        String docA2Email = "docA2-" + UUID.randomUUID() + "@m.test";
        crearUsuario(tenantA, adminAEmail, Rol.ADMIN);
        docenteA1 = crearUsuario(tenantA, docA1Email, Rol.DOCENTE);
        docenteA2 = crearUsuario(tenantA, docA2Email, Rol.DOCENTE);
        adminAToken = login(adminAEmail);
        docenteAToken = login(docA1Email);

        tenantB = nuevaInstitucion("Inst B");
        String adminBEmail = "adminB-" + UUID.randomUUID() + "@m.test";
        UUID docenteB1 = crearUsuario(tenantB, adminBEmail, Rol.ADMIN);
        String docB1Email = "docB1-" + UUID.randomUUID() + "@m.test";
        UUID docB1 = crearUsuario(tenantB, docB1Email, Rol.DOCENTE);
        adminBToken = login(adminBEmail);

        // Uso del tenant A: docente A1 = sonnet (eval) + haiku (narrativa); docente A2 = sonnet (eval).
        // costos: A1 = 4.5 + 0.75 = 5.25 ; A2 = 9.0 ; total A = 14.25 (> umbral 5.00 -> superado).
        insertarUso(tenantA, docenteA1, OperacionLlm.EVALUACION, ModeloTier.POTENTE,
                "claude-sonnet-4-6", 1_000_000, 100_000);
        insertarUso(tenantA, docenteA1, OperacionLlm.NARRATIVA, ModeloTier.ECONOMICO,
                "claude-haiku-4-5", 500_000, 50_000);
        insertarUso(tenantA, docenteA2, OperacionLlm.EVALUACION, ModeloTier.POTENTE,
                "claude-sonnet-4-6", 2_000_000, 200_000);

        // Uso del tenant B (no debe verse desde el admin de A).
        insertarUso(tenantB, docB1, OperacionLlm.EVALUACION, ModeloTier.POTENTE,
                "claude-sonnet-4-6", 3_000_000, 300_000);
    }

    @Test
    void resumenAgregaPorDocenteConCostoTotalYUmbral() throws Exception {
        mvc.perform(get("/api/admin/metricas/uso").header("Authorization", "Bearer " + adminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moneda").value("USD"))
                .andExpect(jsonPath("$.umbralMensual").value(5.00))
                .andExpect(jsonPath("$.umbralSuperado").value(true))
                .andExpect(jsonPath("$.costoTotal").value(14.25))
                .andExpect(jsonPath("$.docentes.length()").value(2))
                // Orden descendente por costo: A2 (9.0) antes que A1 (5.25).
                .andExpect(jsonPath("$.docentes[0].docenteId").value(docenteA2.toString()))
                .andExpect(jsonPath("$.docentes[0].costoEstimado").value(9.0))
                .andExpect(jsonPath("$.docentes[1].docenteId").value(docenteA1.toString()))
                .andExpect(jsonPath("$.docentes[1].costoEstimado").value(5.25))
                .andExpect(jsonPath("$.docentes[1].totalTokens").value(1_650_000));
    }

    @Test
    void aislamientoPorTenant() throws Exception {
        // El admin de B solo ve su institución (un docente con uso), nunca el uso de A.
        mvc.perform(get("/api/admin/metricas/uso").header("Authorization", "Bearer " + adminBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docentes.length()").value(1))
                .andExpect(jsonPath("$.costoTotal").value(13.5));
    }

    @Test
    void detalleDocenteDesglosaPorModeloYOperacion() throws Exception {
        mvc.perform(get("/api/admin/metricas/uso/" + docenteA1)
                        .header("Authorization", "Bearer " + adminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docenteId").value(docenteA1.toString()))
                .andExpect(jsonPath("$.costoTotal").value(5.25))
                .andExpect(jsonPath("$.porModelo.length()").value(2))
                .andExpect(jsonPath("$.porOperacion.length()").value(2));
    }

    @Test
    void mesSinDatosDevuelveVacio() throws Exception {
        mvc.perform(get("/api/admin/metricas/uso").param("mes", "2020-01")
                        .header("Authorization", "Bearer " + adminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2020-01"))
                .andExpect(jsonPath("$.docentes.length()").value(0))
                .andExpect(jsonPath("$.costoTotal").value(0))
                .andExpect(jsonPath("$.umbralSuperado").value(false));
    }

    @Test
    void mesInvalidoEs422() throws Exception {
        mvc.perform(get("/api/admin/metricas/uso").param("mes", "banana")
                        .header("Authorization", "Bearer " + adminAToken))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void docenteNoAccedeAMetricas() throws Exception {
        mvc.perform(get("/api/admin/metricas/uso").header("Authorization", "Bearer " + docenteAToken))
                .andExpect(status().isForbidden());
    }

    // --- seed ---

    private void insertarUso(UUID tenant, UUID docente, OperacionLlm operacion, ModeloTier tier,
            String modelo, long input, long output) {
        TenantContext.set(tenant);
        try {
            RegistroUsoLlm r = new RegistroUsoLlm();
            r.setDocenteId(docente);
            r.setOperacion(operacion);
            r.setTier(tier);
            r.setModelo(modelo);
            r.setInputTokens(input);
            r.setOutputTokens(output);
            registros.save(r);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID nuevaInstitucion(String nombre) {
        TenantContext.clear();
        Institucion i = new Institucion();
        i.setNombre(nombre);
        return instituciones.save(i).getId();
    }

    private UUID crearUsuario(UUID tenant, String email, Rol rol) {
        TenantContext.set(tenant);
        try {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setNombre("Usuario " + rol);
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
