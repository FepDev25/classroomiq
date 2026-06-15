package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;
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
class AuthAndUsuarioFlowTest {

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
    private String adminEmail;

    @BeforeEach
    void seedInstitucionConAdmin() {
        TenantContext.clear();
        Institucion institucion = new Institucion();
        institucion.setNombre("Institución de prueba");
        tenantId = instituciones.save(institucion).getId();

        adminEmail = "admin-" + UUID.randomUUID() + "@test.io";
        crearUsuario(adminEmail, "admin12345", Rol.ADMIN);
    }

    @Test
    void loginEmiteTokenYAdminCreaDocenteEnSuTenant() throws Exception {
        String token = login(adminEmail, "admin12345");

        String docenteEmail = "doc-" + UUID.randomUUID() + "@test.io";
        String body = json.writeValueAsString(Map.of(
                "email", docenteEmail,
                "nombre", "Docente Nuevo",
                "password", "docente123",
                "rol", "DOCENTE"));

        mvc.perform(post("/api/admin/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("DOCENTE"))
                .andExpect(jsonPath("$.activo").value(true));

        // El docente quedó estampado con el tenant del admin.
        TenantContext.runWith(tenantId, () -> {
            Usuario docente = usuarios.findByEmail(docenteEmail).orElseThrow();
            assertThat(docente.getTenantId()).isEqualTo(tenantId);
            assertThat(docente.getRol()).isEqualTo(Rol.DOCENTE);
        });
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        String body = json.writeValueAsString(Map.of("email", adminEmail, "password", "incorrecta"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinContrasenaEsBadRequest() throws Exception {
        String body = json.writeValueAsString(Map.of("email", adminEmail)); // falta password

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void docenteNoPuedeAccederAEndpointsDeAdmin() throws Exception {
        String docenteEmail = "doc-" + UUID.randomUUID() + "@test.io";
        crearUsuario(docenteEmail, "docente123", Rol.DOCENTE);

        String token = login(docenteEmail, "docente123");

        mvc.perform(get("/api/admin/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private String login(String email, String password) throws Exception {
        String body = json.writeValueAsString(Map.of("email", email, "password", password));
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private void crearUsuario(String email, String password, Rol rol) {
        TenantContext.runWith(tenantId, () -> {
            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre("Usuario");
            usuario.setPasswordHash(passwordEncoder.encode(password));
            usuario.setRol(rol);
            usuario.setActivo(true);
            usuarios.save(usuario);
        });
    }
}
