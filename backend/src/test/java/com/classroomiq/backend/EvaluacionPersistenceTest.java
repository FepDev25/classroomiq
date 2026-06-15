package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.FragmentoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.evaluacion.domain.CitaFragmento;
import com.classroomiq.backend.evaluacion.domain.EstadoEvaluacion;
import com.classroomiq.backend.evaluacion.domain.Evaluacion;
import com.classroomiq.backend.evaluacion.domain.EvaluacionCriterio;
import com.classroomiq.backend.evaluacion.repository.CitaFragmentoRepository;
import com.classroomiq.backend.evaluacion.repository.EvaluacionCriterioRepository;
import com.classroomiq.backend.evaluacion.repository.EvaluacionRepository;
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

/**
 * Persistencia del dominio de evaluación (Fase 4, Hito 1) contra un Postgres real: cascada del
 * agregado evaluación → criterios → citas, FK de la cita al fragmento de origen, ciclo de aprobación,
 * reevaluación (borrado por entrega) y aislamiento multi-tenant.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EvaluacionPersistenceTest {

    @Autowired
    private InstitucionRepository instituciones;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private MateriaRepository materias;
    @Autowired
    private RubricaRepository rubricas;
    @Autowired
    private LoteRepository lotes;
    @Autowired
    private EntregaRepository entregas;
    @Autowired
    private FragmentoEntregaRepository fragmentos;
    @Autowired
    private EvaluacionRepository evaluaciones;
    @Autowired
    private EvaluacionCriterioRepository evaluacionCriterios;
    @Autowired
    private CitaFragmentoRepository citas;

    @AfterEach
    void limpiarContexto() {
        TenantContext.clear();
    }

    @Test
    void persisteBorradorConCriterioYCitaYLoAprueba() {
        UUID tenant = nuevaInstitucion("Inst Eval");
        UUID docente = nuevoDocente(tenant, "doc@eval.test");
        UUID materia = nuevaMateria(tenant, docente);
        Rubrica rubrica = nuevaRubricaConCriterio(tenant, docente, materia);
        UUID criterioId = rubrica.getCriterios().get(0).getId();
        UUID nivelId = rubrica.getCriterios().get(0).getNiveles().get(0).getId();

        TenantContext.set(tenant);
        UUID entregaId;
        UUID fragId;
        try {
            entregaId = nuevaEntrega(docente, materia, rubrica.getId());
            fragId = nuevoFragmento(docente, materia, entregaId);

            Evaluacion eval = new Evaluacion();
            eval.setDocenteId(docente);
            eval.setEntregaId(entregaId);
            eval.setRubricaId(rubrica.getId());
            eval.setPuntajeTotalSugerido(new BigDecimal("18.00"));

            EvaluacionCriterio ec = new EvaluacionCriterio();
            ec.setCriterioId(criterioId);
            ec.setEvaluable(true);
            ec.setNivelSugeridoId(nivelId);
            ec.setPuntajeSugerido(new BigDecimal("18.00"));
            ec.setJustificacion("El trabajo calcula O(n log n) en el peor caso con desarrollo.");
            ec.setOrden(0);

            CitaFragmento cita = new CitaFragmento();
            cita.setFragmentoId(fragId);
            cita.setTextoCitado("complejidad O(n log n) en el peor caso");
            cita.setOrden(0);
            ec.addCita(cita);

            eval.addCriterio(ec);
            evaluaciones.save(eval);

            // Round-trip del agregado por entrega (1:1). Los hijos se consultan con sus repos para
            // no navegar colecciones LAZY fuera de sesión (mismo patrón que EntregaPersistenceTest).
            Evaluacion rec = evaluaciones.findByEntregaId(entregaId).orElseThrow();
            assertThat(rec.getEstado()).isEqualTo(EstadoEvaluacion.BORRADOR);
            assertThat(rec.getTenantId()).isEqualTo(tenant);
            assertThat(rec.getPuntajeTotalSugerido()).isEqualByComparingTo("18.00");

            EvaluacionCriterio critRec = evaluacionCriterios
                    .findAllByEvaluacionIdOrderByOrdenAsc(rec.getId()).stream()
                    .findFirst().orElseThrow();
            assertThat(critRec.getCriterioId()).isEqualTo(criterioId);
            assertThat(critRec.getNivelSugeridoId()).isEqualTo(nivelId);
            assertThat(critRec.isEvaluable()).isTrue();
            assertThat(critRec.getJustificacion()).contains("O(n log n)");

            assertThat(citas.findAllByEvaluacionCriterioIdOrderByOrdenAsc(critRec.getId()))
                    .singleElement()
                    .satisfies(ct -> {
                        assertThat(ct.getFragmentoId()).isEqualTo(fragId);
                        assertThat(ct.getTextoCitado()).contains("O(n log n)");
                    });
            assertThat(evaluaciones.existsByEntregaId(entregaId)).isTrue();

            // Aprobación: congela la evaluación final.
            rec.setEstado(EstadoEvaluacion.APROBADA);
            rec.setPuntajeTotalFinal(new BigDecimal("18.00"));
            rec.setComentarioGeneral("Buen análisis, faltó comparar alternativas.");
            rec.setAprobadaAt(Instant.now());
            evaluaciones.save(rec);
            assertThat(evaluaciones.findByEntregaId(entregaId).orElseThrow().getEstado())
                    .isEqualTo(EstadoEvaluacion.APROBADA);

            // Reevaluación: borra el borrador (criterios y citas en cascada).
            evaluaciones.deleteByEntregaId(entregaId);
            assertThat(evaluaciones.findByEntregaId(entregaId)).isEmpty();
            assertThat(evaluacionCriterios.findAllByEvaluacionIdOrderByOrdenAsc(rec.getId())).isEmpty();
            assertThat(citas.findAllByEvaluacionCriterioIdOrderByOrdenAsc(critRec.getId())).isEmpty();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void evaluacionDeUnTenantEsInvisibleParaOtro() {
        UUID tenantA = nuevaInstitucion("Inst A");
        UUID tenantB = nuevaInstitucion("Inst B");
        UUID docenteA = nuevoDocente(tenantA, "doc@a.test");
        UUID materiaA = nuevaMateria(tenantA, docenteA);
        Rubrica rubricaA = nuevaRubricaConCriterio(tenantA, docenteA, materiaA);

        UUID evalId;
        TenantContext.set(tenantA);
        try {
            UUID entregaId = nuevaEntrega(docenteA, materiaA, rubricaA.getId());
            Evaluacion eval = new Evaluacion();
            eval.setDocenteId(docenteA);
            eval.setEntregaId(entregaId);
            eval.setRubricaId(rubricaA.getId());
            EvaluacionCriterio ec = new EvaluacionCriterio();
            ec.setCriterioId(rubricaA.getCriterios().get(0).getId());
            ec.setOrden(0);
            eval.addCriterio(ec);
            evalId = evaluaciones.save(eval).getId();
        } finally {
            TenantContext.clear();
        }

        TenantContext.set(tenantB);
        assertThat(evaluaciones.findById(evalId)).isEmpty();
        assertThat(evaluaciones.findAll()).extracting(Evaluacion::getId).doesNotContain(evalId);
    }

    // --- helpers ---

    private UUID nuevaEntrega(UUID docente, UUID materia, UUID rubrica) {
        Lote lote = new Lote();
        lote.setDocenteId(docente);
        lote.setMateriaId(materia);
        lote.setRubricaId(rubrica);
        lote.setNombre("Lote");
        UUID loteId = lotes.save(lote).getId();

        Entrega entrega = new Entrega();
        entrega.setLoteId(loteId);
        entrega.setDocenteId(docente);
        entrega.setMateriaId(materia);
        entrega.setIdentificadorEstudiante("grupo-01");
        entrega.setTipo(TipoEntrega.DOCUMENTO);
        return entregas.save(entrega).getId();
    }

    private UUID nuevoFragmento(UUID docente, UUID materia, UUID entregaId) {
        Entrega entrega = entregas.findById(entregaId).orElseThrow();
        float[] emb = new float[1024];
        for (int i = 0; i < emb.length; i++) {
            emb[i] = (i % 5) * 0.1f;
        }
        FragmentoEntrega frag = new FragmentoEntrega();
        frag.setDocenteId(docente);
        frag.setMateriaId(materia);
        frag.setLoteId(entrega.getLoteId());
        frag.setEntregaId(entregaId);
        frag.setOrden(0);
        frag.setContenido("El algoritmo tiene complejidad O(n log n) en el peor caso.");
        frag.setSeccion("Análisis de complejidad");
        frag.setEmbedding(emb);
        return fragmentos.save(frag).getId();
    }

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
            usuario.setPasswordHash("no-aplica");
            usuario.setNombre("Docente");
            usuario.setRol(Rol.DOCENTE);
            return usuarios.save(usuario).getId();
        } finally {
            TenantContext.clear();
        }
    }

    private UUID nuevaMateria(UUID tenantId, UUID docenteId) {
        TenantContext.set(tenantId);
        try {
            Materia materia = new Materia();
            materia.setDocenteId(docenteId);
            materia.setNombre("Estructuras de Datos");
            return materias.save(materia).getId();
        } finally {
            TenantContext.clear();
        }
    }

    /** Rúbrica con un criterio y un nivel, para tener ids reales que referenciar desde el borrador. */
    private Rubrica nuevaRubricaConCriterio(UUID tenantId, UUID docenteId, UUID materiaId) {
        TenantContext.set(tenantId);
        try {
            Rubrica rubrica = new Rubrica();
            rubrica.setDocenteId(docenteId);
            rubrica.setMateriaId(materiaId);
            rubrica.setNombre("Rúbrica");
            rubrica.setPuntajeTotal(new BigDecimal("20.00"));
            rubrica.setModoTotal(ModoTotal.SUMA);

            Criterio criterio = new Criterio();
            criterio.setNombre("Análisis de complejidad");
            criterio.setPuntajeMaximo(new BigDecimal("20.00"));
            criterio.setOrden(0);

            NivelDesempeno nivel = new NivelDesempeno();
            nivel.setNombre("Excelente");
            nivel.setTipoPuntaje(TipoPuntaje.RANGO);
            nivel.setPuntajeMin(new BigDecimal("17.00"));
            nivel.setPuntajeMax(new BigDecimal("20.00"));
            nivel.setOrden(0);
            criterio.addNivel(nivel);

            rubrica.addCriterio(criterio);
            return rubricas.save(rubrica);
        } finally {
            TenantContext.clear();
        }
    }
}
