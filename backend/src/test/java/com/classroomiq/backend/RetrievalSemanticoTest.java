package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
import com.classroomiq.backend.entrega.repository.FragmentoSimilar;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.rubrica.domain.ModoTotal;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Retrieval semántico por criterio (Fase 4, Hito 2) contra un Postgres real con pgvector: valida la
 * query nativa de similitud coseno ({@code <=>}) — orden por cercanía y límite top-k —, el acotado
 * por entrega y el aislamiento multi-tenant explícito (el SQL nativo no aplica el filtro de tenant).
 * Usa vectores normalizados construidos a mano para no depender del proveedor de embeddings.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RetrievalSemanticoTest {

    private static final int DIM = 1024;

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

    @AfterEach
    void limpiarContexto() {
        TenantContext.clear();
    }

    @Test
    void recuperaLosFragmentosMasCercanosEnOrdenYRespetaElTopK() {
        UUID tenant = nuevaInstitucion("Inst Retrieval");
        UUID docente = nuevoDocente(tenant, "doc@retrieval.test");
        UUID materia = nuevaMateria(tenant, docente);

        TenantContext.set(tenant);
        try {
            UUID entregaId = nuevaEntrega(docente, materia);
            // f0 idéntico a la consulta (dist 0); f2 a 45° (dist ~0.293); f1 ortogonal (dist 1).
            UUID f0 = nuevoFragmento(docente, materia, entregaId, "exacto", eje(0));
            UUID f1 = nuevoFragmento(docente, materia, entregaId, "ortogonal", eje(1));
            UUID f2 = nuevoFragmento(docente, materia, entregaId, "diagonal", diagonal(0, 1));

            float[] consulta = eje(0);

            List<FragmentoSimilar> top2 = fragmentos.buscarSimilaresEnEntrega(tenant, entregaId, consulta, 2);
            assertThat(top2).extracting(FragmentoSimilar::getId).containsExactly(f0, f2);
            assertThat(top2.get(0).getDistancia()).isLessThan(top2.get(1).getDistancia());
            assertThat(top2.get(0).getDistancia()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-5));
            assertThat(top2.get(0).getContenido()).isEqualTo("exacto");

            List<FragmentoSimilar> todos = fragmentos.buscarSimilaresEnEntrega(tenant, entregaId, consulta, 10);
            assertThat(todos).extracting(FragmentoSimilar::getId).containsExactly(f0, f2, f1);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void noRecuperaFragmentosDeOtraEntregaNiDeOtroTenant() {
        UUID tenantA = nuevaInstitucion("Inst A");
        UUID tenantB = nuevaInstitucion("Inst B");
        UUID docenteA = nuevoDocente(tenantA, "doc@a-retr.test");
        UUID materiaA = nuevaMateria(tenantA, docenteA);
        UUID docenteB = nuevoDocente(tenantB, "doc@b-retr.test");
        UUID materiaB = nuevaMateria(tenantB, docenteB);

        UUID entregaA1;
        UUID fragA1;
        TenantContext.set(tenantA);
        try {
            entregaA1 = nuevaEntrega(docenteA, materiaA);
            UUID entregaA2 = nuevaEntrega(docenteA, materiaA);
            fragA1 = nuevoFragmento(docenteA, materiaA, entregaA1, "de A1", eje(0));
            nuevoFragmento(docenteA, materiaA, entregaA2, "de A2", eje(0));
        } finally {
            TenantContext.clear();
        }

        UUID entregaB;
        TenantContext.set(tenantB);
        try {
            entregaB = nuevaEntrega(docenteB, materiaB);
            nuevoFragmento(docenteB, materiaB, entregaB, "de B", eje(0));
        } finally {
            TenantContext.clear();
        }

        float[] consulta = eje(0);

        // Acotado a entregaA1: no trae el fragmento de la otra entrega del mismo docente.
        TenantContext.set(tenantA);
        List<FragmentoSimilar> soloA1 = fragmentos.buscarSimilaresEnEntrega(tenantA, entregaA1, consulta, 10);
        assertThat(soloA1).extracting(FragmentoSimilar::getId).containsExactly(fragA1);

        // El tenant B no ve la entrega de A aunque pase su id (filtro explícito de tenant_id).
        List<FragmentoSimilar> cruzado = fragmentos.buscarSimilaresEnEntrega(tenantB, entregaA1, consulta, 10);
        assertThat(cruzado).isEmpty();
    }

    // --- helpers ---

    private static float[] eje(int idx) {
        float[] v = new float[DIM];
        v[idx] = 1.0f;
        return v;
    }

    private static float[] diagonal(int a, int b) {
        float[] v = new float[DIM];
        float n = (float) (1.0 / Math.sqrt(2));
        v[a] = n;
        v[b] = n;
        return v;
    }

    private UUID nuevaEntrega(UUID docente, UUID materia) {
        Rubrica rubrica = new Rubrica();
        rubrica.setDocenteId(docente);
        rubrica.setMateriaId(materia);
        rubrica.setNombre("Rúbrica");
        rubrica.setPuntajeTotal(new java.math.BigDecimal("20.00"));
        rubrica.setModoTotal(ModoTotal.SUMA);
        UUID rubricaId = rubricas.save(rubrica).getId();

        Lote lote = new Lote();
        lote.setDocenteId(docente);
        lote.setMateriaId(materia);
        lote.setRubricaId(rubricaId);
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

    private UUID nuevoFragmento(UUID docente, UUID materia, UUID entregaId, String contenido, float[] emb) {
        Entrega entrega = entregas.findById(entregaId).orElseThrow();
        FragmentoEntrega frag = new FragmentoEntrega();
        frag.setDocenteId(docente);
        frag.setMateriaId(materia);
        frag.setLoteId(entrega.getLoteId());
        frag.setEntregaId(entregaId);
        frag.setOrden(0);
        frag.setContenido(contenido);
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
}
