package com.classroomiq.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stream SSE de progreso (Hito 6): el dueño del lote puede abrir el stream (la respuesta entra en
 * modo asíncrono SSE) y un docente ajeno recibe 404 — la autorización corre en el hilo del request
 * leyendo tenant + docente, antes de devolver el {@code Flux}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SseEventosTest {

    private static final String RUBRICA_VALIDA = """
            {
              "nombre": "Rúbrica SSE",
              "puntajeTotal": 10,
              "modoTotal": "SUMA",
              "criterios": [
                {"nombre": "Criterio", "puntajeMaximo": 10, "niveles": [
                  {"nombre": "Excelente", "tipoPuntaje": "RANGO", "puntajeMin": 6, "puntajeMax": 10},
                  {"nombre": "Insuficiente", "tipoPuntaje": "RANGO", "puntajeMin": 0, "puntajeMax": 5}]}
              ]
            }
            """;

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

    private UUID tenantId;

    @BeforeEach
    void seedInstitucion() {
        TenantContext.clear();
        Institucion institucion = new Institucion();
        institucion.setNombre("Institución SSE");
        tenantId = instituciones.save(institucion).getId();
    }

    @Test
    void dueñoAbreElStreamDeEventos() throws Exception {
        String token = login(nuevoDocente());
        UUID materiaId = crearMateria(token, "Algoritmos");
        UUID loteId = crearLote(token, materiaId, crearRubrica(token, materiaId));

        // El controller devuelve un Flux SSE → el request entra en modo asíncrono (autorizado).
        mvc.perform(get("/api/lotes/" + loteId + "/eventos")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted());
    }

    @Test
    void docenteAjenoNoSeSuscribeALoteDeOtro() throws Exception {
        String tokenA = login(nuevoDocente());
        UUID materiaA = crearMateria(tokenA, "Redes");
        UUID loteDeA = crearLote(tokenA, materiaA, crearRubrica(tokenA, materiaA));

        String tokenB = login(nuevoDocente());
        mvc.perform(get("/api/lotes/" + loteDeA + "/eventos")
                        .header("Authorization", "Bearer " + tokenB)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinTokenElStreamEsRechazado() throws Exception {
        mvc.perform(get("/api/lotes/" + UUID.randomUUID() + "/eventos")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private UUID crearMateria(String token, String nombre) throws Exception {
        MvcResult r = mvc.perform(post("/api/materias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID crearRubrica(String token, UUID materiaId) throws Exception {
        MvcResult r = mvc.perform(post("/api/materias/" + materiaId + "/rubricas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RUBRICA_VALIDA))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID crearLote(String token, UUID materiaId, UUID rubricaId) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "materiaId", materiaId, "rubricaId", rubricaId, "nombre", "Lote SSE"));
        MvcResult r = mvc.perform(post("/api/lotes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private String login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "docente123"))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String nuevoDocente() {
        String email = "doc-" + UUID.randomUUID() + "@test.io";
        TenantContext.runWith(tenantId, () -> {
            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre("Docente");
            usuario.setPasswordHash(passwordEncoder.encode("docente123"));
            usuario.setRol(Rol.DOCENTE);
            usuario.setActivo(true);
            usuarios.save(usuario);
        });
        return email;
    }
}
