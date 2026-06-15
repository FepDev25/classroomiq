package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Verifica el aislamiento multi-tenant contra un Postgres real:
 * cruce de tenants imposible, estampado automático de tenant_id, scoping por docente,
 * y comportamiento fail-closed cuando no hay tenant fijado.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MultiTenantIsolationTest {

    @Autowired
    private InstitucionRepository instituciones;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private MateriaRepository materias;

    @AfterEach
    void limpiarContexto() {
        TenantContext.clear();
    }

    @Test
    void materiaDeUnTenantEsInvisibleParaOtroTenant() {
        UUID tenantA = nuevaInstitucion("Inst A");
        UUID tenantB = nuevaInstitucion("Inst B");
        UUID docenteA = nuevoDocente(tenantA, "docA@inst-a.test");
        UUID materiaId = nuevaMateria(tenantA, docenteA, "Estructuras de Datos");

        // Visible y correctamente estampada para su propio tenant.
        TenantContext.set(tenantA);
        Materia recuperada = materias.findById(materiaId).orElseThrow();
        assertThat(recuperada.getTenantId()).isEqualTo(tenantA);
        TenantContext.clear();

        // Invisible para otro tenant, incluso por id directo.
        TenantContext.set(tenantB);
        assertThat(materias.findById(materiaId)).isEmpty();
        assertThat(materias.findAll()).extracting(Materia::getId).doesNotContain(materiaId);
    }

    @Test
    void docenteNoVeMateriasDeOtroDocenteDelMismoTenant() {
        UUID tenant = nuevaInstitucion("Inst C");
        UUID docente1 = nuevoDocente(tenant, "doc1@inst-c.test");
        UUID docente2 = nuevoDocente(tenant, "doc2@inst-c.test");
        UUID materiaDocente1 = nuevaMateria(tenant, docente1, "Bases de Datos");

        TenantContext.set(tenant);
        assertThat(materias.findAllByDocenteId(docente1))
                .extracting(Materia::getId)
                .containsExactly(materiaDocente1);
        assertThat(materias.findAllByDocenteId(docente2)).isEmpty();
        assertThat(materias.findByIdAndDocenteId(materiaDocente1, docente2)).isEmpty();
        assertThat(materias.findByIdAndDocenteId(materiaDocente1, docente1)).isPresent();
    }

    @Test
    void sinTenantFijadoNoSeVeNingunaMateria() {
        UUID tenant = nuevaInstitucion("Inst D");
        UUID docente = nuevoDocente(tenant, "doc@inst-d.test");
        nuevaMateria(tenant, docente, "Ingeniería de Software");

        TenantContext.clear();
        assertThat(materias.findAll()).isEmpty();
    }

    // --- helpers ---

    private UUID nuevaInstitucion(String nombre) {
        TenantContext.clear();
        Institucion institucion = new Institucion();
        institucion.setNombre(nombre);
        return instituciones.save(institucion).getId();
    }

    private UUID nuevoDocente(UUID tenantId, String email) {
        TenantContext.set(tenantId);
        try {
            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setPasswordHash("no-aplica-en-este-test");
            usuario.setNombre("Docente");
            usuario.setRol(Rol.DOCENTE);
            return usuarios.save(usuario).getId();
        } finally {
            TenantContext.clear();
        }
    }

    private UUID nuevaMateria(UUID tenantId, UUID docenteId, String nombre) {
        TenantContext.set(tenantId);
        try {
            Materia materia = new Materia();
            materia.setDocenteId(docenteId);
            materia.setNombre(nombre);
            // No fijamos tenantId a propósito: lo debe estampar Hibernate desde el TenantContext.
            return materias.save(materia).getId();
        } finally {
            TenantContext.clear();
        }
    }
}
