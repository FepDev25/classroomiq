package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Verifica el seed end-to-end: arranca con {@code app.seed.enabled=true} (sobrescribe el default
 * de los tests) y comprueba que la siembra parsea las 4 rúbricas de ejemplo y crea los datos.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@Import(TestcontainersConfiguration.class)
class SeederTest {

    @Autowired
    private InstitucionRepository instituciones;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private MateriaRepository materias;

    @Autowired
    private RubricaRepository rubricas;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    void elSeedCargaInstitucionUsuariosYRubricas() {
        UUID tenantId = instituciones.findAll().stream()
                .filter(institucion -> institucion.getNombre().equals("Institución Demo"))
                .findFirst()
                .orElseThrow()
                .getId();

        TenantContext.set(tenantId);
        try {
            assertThat(rubricas.count()).isEqualTo(4);
            assertThat(materias.count()).isEqualTo(3);
            assertThat(usuarios.findByEmail("admin@demo.local"))
                    .get().extracting(Usuario::getRol).isEqualTo(Rol.ADMIN);
            assertThat(usuarios.findByEmail("docente@demo.local")).isPresent();
        } finally {
            TenantContext.clear();
        }

        // La rúbrica de FotoFlux conserva el criterio marcado como no evaluable por contenido.
        TenantContext.set(tenantId);
        try {
            new TransactionTemplate(txManager).executeWithoutResult(status -> {
                Rubrica fotoflux = rubricas.findAll().stream()
                        .filter(rubrica -> rubrica.getNombre().toLowerCase().contains("fotoflux"))
                        .findFirst()
                        .orElseThrow();
                assertThat(fotoflux.getCriterios())
                        .anyMatch(criterio -> !criterio.isEvaluablePorContenido());
            });
        } finally {
            TenantContext.clear();
        }
    }
}
