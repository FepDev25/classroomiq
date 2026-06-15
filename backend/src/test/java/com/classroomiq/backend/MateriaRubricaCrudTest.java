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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MateriaRubricaCrudTest {

    private static final String RUBRICA_VALIDA = """
            {
              "nombre": "Rúbrica de prueba",
              "descripcion": "Entrega documento",
              "puntajeTotal": 10,
              "modoTotal": "SUMA",
              "criterios": [
                {"nombre": "Definición del problema", "puntajeMaximo": 6, "niveles": [
                  {"nombre": "Excelente", "tipoPuntaje": "RANGO", "puntajeMin": 5, "puntajeMax": 6},
                  {"nombre": "Insuficiente", "tipoPuntaje": "RANGO", "puntajeMin": 0, "puntajeMax": 4}]},
                {"nombre": "Claridad", "puntajeMaximo": 4, "niveles": [
                  {"nombre": "Excelente", "tipoPuntaje": "RANGO", "puntajeMin": 3, "puntajeMax": 4},
                  {"nombre": "Insuficiente", "tipoPuntaje": "RANGO", "puntajeMin": 0, "puntajeMax": 2}]}
              ]
            }
            """;

    // Igual que la válida pero el total (12) no coincide con la suma de criterios (10).
    private static final String RUBRICA_INCOHERENTE = RUBRICA_VALIDA.replace("\"puntajeTotal\": 10", "\"puntajeTotal\": 12");

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
        institucion.setNombre("Institución de prueba");
        tenantId = instituciones.save(institucion).getId();
    }

    @Test
    void docenteCreaMateriaYRubrica() throws Exception {
        String email = nuevoDocente();
        String token = login(email);

        UUID materiaId = crearMateria(token, "Ingeniería de Software");

        mvc.perform(get("/api/materias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        MvcResult creada = mvc.perform(post("/api/materias/" + materiaId + "/rubricas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RUBRICA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.criterios.length()").value(2))
                .andReturn();

        UUID rubricaId = UUID.fromString(
                json.readTree(creada.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/api/rubricas/" + rubricaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modoTotal").value("SUMA"))
                .andExpect(jsonPath("$.criterios.length()").value(2))
                .andExpect(jsonPath("$.criterios[0].niveles.length()").value(2))
                .andExpect(jsonPath("$.criterios[0].evaluablePorContenido").value(true));
    }

    @Test
    void rubricaConPuntajesIncoherentesEsRechazada() throws Exception {
        String email = nuevoDocente();
        String token = login(email);
        UUID materiaId = crearMateria(token, "Estructuras de Datos");

        mvc.perform(post("/api/materias/" + materiaId + "/rubricas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RUBRICA_INCOHERENTE))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void docenteNoVeMateriaDeOtroDocente() throws Exception {
        String emailA = nuevoDocente();
        String tokenA = login(emailA);
        UUID materiaDeA = crearMateria(tokenA, "Bases de Datos");

        String emailB = nuevoDocente();
        String tokenB = login(emailB);

        mvc.perform(get("/api/materias/" + materiaDeA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/materias").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- helpers ---

    private UUID crearMateria(String token, String nombre) throws Exception {
        String body = json.writeValueAsString(Map.of("nombre", nombre));
        MvcResult result = mvc.perform(post("/api/materias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String login(String email) throws Exception {
        String body = json.writeValueAsString(Map.of("email", email, "password", "docente123"));
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
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
