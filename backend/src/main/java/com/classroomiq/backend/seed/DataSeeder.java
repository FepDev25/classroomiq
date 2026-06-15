package com.classroomiq.backend.seed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.ModoTotal;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.domain.TipoPuntaje;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Siembra datos de demo al arrancar: una institución, un admin, un docente y las rúbricas de
 * ejemplo (classpath {@code seed/rubricas/*.json}, formato de data/rubricas-ejemplo/SCHEMA.md).
 * Idempotente: si ya hay alguna institución, no hace nada. Se activa con {@code app.seed.enabled}.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String ADMIN_EMAIL = "admin@demo.local";
    private static final String ADMIN_PASSWORD = "admin12345";
    private static final String DOCENTE_EMAIL = "docente@demo.local";
    private static final String DOCENTE_PASSWORD = "docente12345";

    private final InstitucionRepository instituciones;
    private final UsuarioRepository usuarios;
    private final MateriaRepository materias;
    private final RubricaRepository rubricas;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataSeeder(InstitucionRepository instituciones, UsuarioRepository usuarios, MateriaRepository materias,
            RubricaRepository rubricas, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.instituciones = instituciones;
        this.usuarios = usuarios;
        this.materias = materias;
        this.rubricas = rubricas;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!instituciones.findAll().isEmpty()) {
            log.info("Seed: ya existen datos, se omite la siembra");
            return;
        }

        List<SeedRubrica> seeds = cargarRubricas();

        Institucion institucion = new Institucion();
        institucion.setNombre("Institución Demo");
        UUID tenantId = instituciones.save(institucion).getId();

        TenantContext.runWith(tenantId, () -> sembrarTenant(seeds));

        log.info("Seed completo. Institución 'Institución Demo' con {} rúbricas de ejemplo.", seeds.size());
        log.info("Seed: admin  -> {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
        log.info("Seed: docente -> {} / {}", DOCENTE_EMAIL, DOCENTE_PASSWORD);
    }

    private void sembrarTenant(List<SeedRubrica> seeds) {
        crearUsuario(ADMIN_EMAIL, ADMIN_PASSWORD, "Administrador Demo", Rol.ADMIN);
        UUID docenteId = crearUsuario(DOCENTE_EMAIL, DOCENTE_PASSWORD, "Docente Demo", Rol.DOCENTE);

        Map<String, UUID> materiaPorNombre = new HashMap<>();
        for (SeedRubrica seed : seeds) {
            UUID materiaId = materiaPorNombre.computeIfAbsent(seed.materia(),
                    nombre -> crearMateria(docenteId, nombre));
            rubricas.save(construirRubrica(seed, docenteId, materiaId));
        }
    }

    private UUID crearUsuario(String email, String password, String nombre, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuarios.save(usuario).getId();
    }

    private UUID crearMateria(UUID docenteId, String nombre) {
        Materia materia = new Materia();
        materia.setDocenteId(docenteId);
        materia.setNombre(nombre);
        materia.setPeriodoAcademico("Demo");
        return materias.save(materia).getId();
    }

    private Rubrica construirRubrica(SeedRubrica seed, UUID docenteId, UUID materiaId) {
        Rubrica rubrica = new Rubrica();
        rubrica.setDocenteId(docenteId);
        rubrica.setMateriaId(materiaId);
        rubrica.setNombre(seed.nombre());
        rubrica.setDescripcion(seed.descripcion());
        rubrica.setPuntajeTotal(seed.puntajeTotal());
        rubrica.setModoTotal(ModoTotal.valueOf(seed.modoTotal().trim().toUpperCase()));

        int ordenCriterio = 0;
        for (SeedCriterio seedCriterio : seed.criterios()) {
            Criterio criterio = new Criterio();
            criterio.setNombre(seedCriterio.nombre());
            criterio.setDescripcion(seedCriterio.descripcion());
            criterio.setPuntajeMaximo(seedCriterio.puntajeMaximo());
            criterio.setEvaluablePorContenido(
                    seedCriterio.evaluablePorContenido() == null || seedCriterio.evaluablePorContenido());
            criterio.setOrden(ordenCriterio++);

            int ordenNivel = 0;
            for (SeedNivel seedNivel : seedCriterio.niveles()) {
                criterio.addNivel(construirNivel(seedNivel, ordenNivel++));
            }
            rubrica.addCriterio(criterio);
        }
        return rubrica;
    }

    private NivelDesempeno construirNivel(SeedNivel seed, int orden) {
        NivelDesempeno nivel = new NivelDesempeno();
        nivel.setNombre(seed.nombre());
        nivel.setDescripcion(seed.descripcion());
        nivel.setOrden(orden);

        TipoPuntaje tipo = tipoPuntaje(seed.tipoPuntaje());
        nivel.setTipoPuntaje(tipo);
        switch (tipo) {
            case RANGO -> {
                nivel.setPuntajeMin(seed.min());
                nivel.setPuntajeMax(seed.max());
            }
            case FIJO -> nivel.setPuntajeValor(seed.valor());
            case BANDA_PCT -> {
                nivel.setPctMin(seed.pctMin());
                nivel.setPctMax(seed.pctMax());
            }
        }
        return nivel;
    }

    private static TipoPuntaje tipoPuntaje(String valor) {
        return switch (valor.trim().toLowerCase()) {
            case "rango" -> TipoPuntaje.RANGO;
            case "fijo" -> TipoPuntaje.FIJO;
            case "bandapct" -> TipoPuntaje.BANDA_PCT;
            default -> throw new IllegalArgumentException("tipoPuntaje desconocido en el seed: " + valor);
        };
    }

    private List<SeedRubrica> cargarRubricas() throws Exception {
        Resource[] recursos = new PathMatchingResourcePatternResolver()
                .getResources("classpath:seed/rubricas/*.json");
        List<SeedRubrica> seeds = new ArrayList<>();
        for (Resource recurso : recursos) {
            try (var input = recurso.getInputStream()) {
                seeds.add(objectMapper.readValue(input, SeedRubrica.class));
            }
        }
        return seeds;
    }

    // --- formato de los JSON de data/rubricas-ejemplo/ (campos por tipo de puntaje) ---

    record SeedRubrica(String nombre, String materia, String descripcion,
            BigDecimal puntajeTotal, String modoTotal, List<SeedCriterio> criterios) {
    }

    record SeedCriterio(String nombre, String descripcion, BigDecimal puntajeMaximo,
            Boolean evaluablePorContenido, List<SeedNivel> niveles) {
    }

    record SeedNivel(String nombre, String descripcion, String tipoPuntaje,
            BigDecimal min, BigDecimal max, BigDecimal valor, BigDecimal pctMin, BigDecimal pctMax) {
    }
}
