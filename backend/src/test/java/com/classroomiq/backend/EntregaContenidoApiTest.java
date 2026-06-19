package com.classroomiq.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
 * Endpoint de contenido de entrega ({@code GET /api/entregas/{id}/contenido}): reconstruye el texto
 * completo por re-extracción de los archivos en disco (sin LLM/embeddings) para el panel de revisión.
 * Verifica el texto devuelto por archivo/sección y el aislamiento por docente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EntregaContenidoApiTest {

    private static final String CODIGO_PY = "def saludar():\n    return 'hola classroomiq'\n";

    private static final String RUBRICA_VALIDA = """
            {
              "nombre": "Rúbrica de prueba",
              "puntajeTotal": 10,
              "modoTotal": "SUMA",
              "criterios": [
                {"nombre": "Criterio", "puntajeMaximo": 10, "niveles": [
                  {"nombre": "Excelente", "tipoPuntaje": "RANGO", "puntajeMin": 6, "puntajeMax": 10},
                  {"nombre": "Insuficiente", "tipoPuntaje": "RANGO", "puntajeMin": 0, "puntajeMax": 5}]}
              ]
            }
            """;

    static Path storageDir;

    @DynamicPropertySource
    static void storageProps(DynamicPropertyRegistry registry) throws IOException {
        storageDir = Files.createTempDirectory("classroomiq-contenido-storage");
        registry.add("app.storage.base-path", storageDir::toString);
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

    private UUID tenantId;

    @BeforeEach
    void seedInstitucion() {
        TenantContext.clear();
        Institucion institucion = new Institucion();
        institucion.setNombre("Institución de prueba");
        tenantId = instituciones.save(institucion).getId();
    }

    @Test
    void devuelveElTextoReconstruidoDeLaEntrega() throws Exception {
        String token = login(nuevoDocente());
        UUID materiaId = crearMateria(token, "Programación I");
        UUID rubricaId = crearRubrica(token, materiaId);
        UUID loteId = crearLote(token, materiaId, rubricaId);
        UUID entregaId = subirZipCodigo(token, loteId);

        mvc.perform(get("/api/entregas/" + entregaId + "/contenido")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entregaId").value(entregaId.toString()))
                .andExpect(jsonPath("$.archivos.length()").value(1))
                .andExpect(jsonPath("$.archivos[0].nombreOriginal").value("proyecto.zip"))
                .andExpect(jsonPath("$.archivos[0].rol").value("CODIGO"))
                .andExpect(jsonPath("$.archivos[0].secciones.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.archivos[0].secciones[0].texto")
                        .value(org.hamcrest.Matchers.containsString("def saludar")));
    }

    @Test
    void noExponeElContenidoDeLaEntregaDeOtroDocente() throws Exception {
        String tokenA = login(nuevoDocente());
        UUID materiaA = crearMateria(tokenA, "Algoritmos");
        UUID rubricaA = crearRubrica(tokenA, materiaA);
        UUID loteA = crearLote(tokenA, materiaA, rubricaA);
        UUID entregaDeA = subirZipCodigo(tokenA, loteA);

        String tokenB = login(nuevoDocente());
        mvc.perform(get("/api/entregas/" + entregaDeA + "/contenido")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private UUID subirZipCodigo(String token, UUID loteId) throws Exception {
        MockMultipartFile zip = new MockMultipartFile(
                "archivos", "proyecto.zip", "application/zip", zipConArchivo("main.py", CODIGO_PY));
        MvcResult res = mvc.perform(multipart("/api/lotes/" + loteId + "/entregas")
                        .file(zip)
                        .param("identificadorEstudiante", "grupo-01")
                        .param("tipo", "CODIGO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    private static byte[] zipConArchivo(String nombre, String contenido) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(nombre));
            zip.write(contenido.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private UUID crearMateria(String token, String nombre) throws Exception {
        MvcResult result = mvc.perform(post("/api/materias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID crearRubrica(String token, UUID materiaId) throws Exception {
        MvcResult result = mvc.perform(post("/api/materias/" + materiaId + "/rubricas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RUBRICA_VALIDA))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID crearLote(String token, UUID materiaId, UUID rubricaId) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "materiaId", materiaId, "rubricaId", rubricaId, "nombre", "Proyecto Final — Grupo A"));
        MvcResult result = mvc.perform(post("/api/lotes")
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
