package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

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
 * Flujo de subida de entregas (Fase 3, Hito 2): crear lote, subir archivos multipart, validar
 * coherencia tipo/archivos, persistencia en disco, aislamiento por docente y borrado en cascada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EntregaUploadTest {

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
        storageDir = Files.createTempDirectory("classroomiq-test-storage");
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
    void seedInstitucion() throws IOException {
        TenantContext.clear();
        Institucion institucion = new Institucion();
        institucion.setNombre("Institución de prueba");
        tenantId = instituciones.save(institucion).getId();
        limpiarStorage();
    }

    /** storageDir es estático (compartido entre tests): lo vaciamos para contar solo este test. */
    private void limpiarStorage() throws IOException {
        if (!Files.exists(storageDir)) {
            return;
        }
        try (Stream<Path> rutas = Files.walk(storageDir)) {
            rutas.sorted(java.util.Comparator.reverseOrder())
                    .filter(p -> !p.equals(storageDir))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    });
        }
    }

    @Test
    void docenteSubeEntregaDocumentoYSeGuardaEnDisco() throws Exception {
        String token = login(nuevoDocente());
        UUID materiaId = crearMateria(token, "Ingeniería de Software");
        UUID rubricaId = crearRubrica(token, materiaId);
        UUID loteId = crearLote(token, materiaId, rubricaId);

        MockMultipartFile pdf = new MockMultipartFile(
                "archivos", "informe.pdf", "application/pdf", "contenido de prueba".getBytes());

        MvcResult res = mvc.perform(multipart("/api/lotes/" + loteId + "/entregas")
                        .file(pdf)
                        .param("identificadorEstudiante", "grupo-01")
                        .param("tipo", "DOCUMENTO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.tipo").value("DOCUMENTO"))
                .andExpect(jsonPath("$.archivos.length()").value(1))
                .andExpect(jsonPath("$.archivos[0].rol").value("DOCUMENTO"))
                .andExpect(jsonPath("$.archivos[0].nombreOriginal").value("informe.pdf"))
                .andReturn();

        // El binario quedó en disco bajo la ruta del tenant.
        assertThat(archivosEnDisco()).isEqualTo(1);

        UUID entregaId = UUID.fromString(
                json.readTree(res.getResponse().getContentAsString()).get("id").asText());
        mvc.perform(get("/api/entregas/" + entregaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivos.length()").value(1));
    }

    @Test
    void entregaDocumentoConZipEsRechazada() throws Exception {
        String token = login(nuevoDocente());
        UUID materiaId = crearMateria(token, "Estructuras de Datos");
        UUID rubricaId = crearRubrica(token, materiaId);
        UUID loteId = crearLote(token, materiaId, rubricaId);

        MockMultipartFile zip = new MockMultipartFile(
                "archivos", "proyecto.zip", "application/zip", "PK..".getBytes());

        mvc.perform(multipart("/api/lotes/" + loteId + "/entregas")
                        .file(zip)
                        .param("identificadorEstudiante", "grupo-02")
                        .param("tipo", "DOCUMENTO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());

        assertThat(archivosEnDisco()).isZero();
    }

    @Test
    void docenteNoSubeEntregaAlLoteDeOtroDocente() throws Exception {
        String tokenA = login(nuevoDocente());
        UUID materiaA = crearMateria(tokenA, "Bases de Datos");
        UUID rubricaA = crearRubrica(tokenA, materiaA);
        UUID loteDeA = crearLote(tokenA, materiaA, rubricaA);

        String tokenB = login(nuevoDocente());
        MockMultipartFile pdf = new MockMultipartFile(
                "archivos", "informe.pdf", "application/pdf", "x".getBytes());

        mvc.perform(multipart("/api/lotes/" + loteDeA + "/entregas")
                        .file(pdf)
                        .param("identificadorEstudiante", "grupo-03")
                        .param("tipo", "DOCUMENTO")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/lotes/" + loteDeA + "/entregas")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrarLoteEliminaLosArchivosDelDisco() throws Exception {
        String token = login(nuevoDocente());
        UUID materiaId = crearMateria(token, "Sistemas Operativos");
        UUID rubricaId = crearRubrica(token, materiaId);
        UUID loteId = crearLote(token, materiaId, rubricaId);

        MockMultipartFile pdf = new MockMultipartFile(
                "archivos", "informe.pdf", "application/pdf", "datos".getBytes());
        mvc.perform(multipart("/api/lotes/" + loteId + "/entregas")
                        .file(pdf)
                        .param("identificadorEstudiante", "grupo-04")
                        .param("tipo", "DOCUMENTO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        assertThat(archivosEnDisco()).isEqualTo(1);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/lotes/" + loteId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(archivosEnDisco()).isZero();
    }

    private long archivosEnDisco() throws IOException {
        if (!Files.exists(storageDir)) {
            return 0;
        }
        try (Stream<Path> rutas = Files.walk(storageDir)) {
            return rutas.filter(Files::isRegularFile).count();
        }
    }

    // --- helpers ---

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
