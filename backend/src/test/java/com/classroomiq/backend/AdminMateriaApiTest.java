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
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Catálogo de materias para el admin (H3 v2): el admin ve TODAS las materias del tenant con su
 * docente dueño (sin filtrar por docente), y un docente no puede acceder a este endpoint (403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminMateriaApiTest {

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

    private UUID tenantId;
    private UUID adaId;
    private UUID linusId;
    private String adminToken;
    private String docenteToken;

    @BeforeEach
    void preparar() {
        TenantContext.clear();
        Institucion inst = new Institucion();
        inst.setNombre("Inst Materias");
        tenantId = instituciones.save(inst).getId();

        String adminEmail = "admin-" + UUID.randomUUID() + "@m.test";
        String adaEmail = "ada-" + UUID.randomUUID() + "@m.test";
        String linusEmail = "linus-" + UUID.randomUUID() + "@m.test";
        crearUsuario(adminEmail, "Admin", Rol.ADMIN);
        adaId = crearUsuario(adaEmail, "Ada Lovelace", Rol.DOCENTE);
        linusId = crearUsuario(linusEmail, "Linus Torvalds", Rol.DOCENTE);
        adminToken = login(adminEmail);
        docenteToken = login(adaEmail);

        crearMateria(adaId, "Algoritmos");
        crearMateria(linusId, "Sistemas Operativos");
    }

    @Test
    void adminVeTodasLasMateriasConSuDocente() throws Exception {
        mvc.perform(get("/api/admin/materias").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Orden por nombre de docente: Ada antes que Linus.
                .andExpect(jsonPath("$[0].nombre").value("Algoritmos"))
                .andExpect(jsonPath("$[0].docenteNombre").value("Ada Lovelace"))
                .andExpect(jsonPath("$[0].docenteId").value(adaId.toString()))
                .andExpect(jsonPath("$[1].nombre").value("Sistemas Operativos"))
                .andExpect(jsonPath("$[1].docenteNombre").value("Linus Torvalds"))
                .andExpect(jsonPath("$[1].docenteId").value(linusId.toString()));
    }

    @Test
    void docenteNoAccedeAlCatalogoAdmin() throws Exception {
        mvc.perform(get("/api/admin/materias").header("Authorization", "Bearer " + docenteToken))
                .andExpect(status().isForbidden());
    }

    private UUID crearUsuario(String email, String nombre, Rol rol) {
        TenantContext.set(tenantId);
        try {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setNombre(nombre);
            u.setPasswordHash(passwordEncoder.encode("clave123"));
            u.setRol(rol);
            u.setActivo(true);
            return usuarios.save(u).getId();
        } finally {
            TenantContext.clear();
        }
    }

    private void crearMateria(UUID docenteId, String nombre) {
        TenantContext.set(tenantId);
        try {
            Materia m = new Materia();
            m.setDocenteId(docenteId);
            m.setNombre(nombre);
            materias.save(m);
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
