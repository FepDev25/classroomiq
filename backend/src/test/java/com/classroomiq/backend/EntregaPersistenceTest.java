package com.classroomiq.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.FragmentoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.domain.RolArchivo;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.repository.ArchivoEntregaRepository;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
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
 * Persistencia del dominio de entregas (Fase 3, Hito 1) contra un Postgres+pgvector real:
 * round-trip del embedding vector(1024), cascada de archivos en el agregado entrega, y
 * aislamiento multi-tenant de lotes/entregas/fragmentos.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EntregaPersistenceTest {

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
    private ArchivoEntregaRepository archivos;
    @Autowired
    private FragmentoEntregaRepository fragmentos;

    @AfterEach
    void limpiarContexto() {
        TenantContext.clear();
    }

    @Test
    void persisteEntregaConArchivosYFragmentoVectorial() {
        UUID tenant = nuevaInstitucion("Inst Entregas");
        UUID docente = nuevoDocente(tenant, "doc@entregas.test");
        UUID materia = nuevaMateria(tenant, docente);
        UUID rubrica = nuevaRubrica(tenant, docente, materia);

        TenantContext.set(tenant);
        try {
            // Lote -> entrega -> archivo (cascada).
            Lote lote = new Lote();
            lote.setDocenteId(docente);
            lote.setMateriaId(materia);
            lote.setRubricaId(rubrica);
            lote.setNombre("Proyecto Final — Grupo A");
            UUID loteId = lotes.save(lote).getId();

            Entrega entrega = new Entrega();
            entrega.setLoteId(loteId);
            entrega.setDocenteId(docente);
            entrega.setMateriaId(materia);
            entrega.setIdentificadorEstudiante("grupo-01");
            entrega.setTipo(TipoEntrega.MIXTA);
            ArchivoEntrega archivo = new ArchivoEntrega();
            archivo.setNombreOriginal("informe.pdf");
            archivo.setRutaRelativa(tenant + "/" + materia + "/" + loteId + "/informe.pdf");
            archivo.setMimeType("application/pdf");
            archivo.setTamanoBytes(12345L);
            archivo.setHashSha256("abc123");
            archivo.setRol(RolArchivo.DOCUMENTO);
            archivo.setOrden(0);
            entrega.addArchivo(archivo);
            UUID entregaId = entregas.save(entrega).getId();

            // Fragmento con embedding 1024-dim.
            float[] emb = nuevoEmbedding();
            FragmentoEntrega frag = new FragmentoEntrega();
            frag.setDocenteId(docente);
            frag.setMateriaId(materia);
            frag.setLoteId(loteId);
            frag.setEntregaId(entregaId);
            frag.setOrden(0);
            frag.setContenido("El algoritmo tiene complejidad O(n log n) en el peor caso.");
            frag.setSeccion("Análisis de complejidad");
            frag.setLineaInicio(10);
            frag.setLineaFin(24);
            frag.setEmbedding(emb);
            UUID fragId = fragmentos.save(frag).getId();

            // Round-trip: el vector vuelve idéntico desde pgvector.
            FragmentoEntrega recuperado = fragmentos.findById(fragId).orElseThrow();
            assertThat(recuperado.getEmbedding()).hasSize(1024);
            assertThat(recuperado.getEmbedding()).usingComparatorWithPrecision(1e-6f).containsExactly(emb);
            assertThat(recuperado.getTenantId()).isEqualTo(tenant);

            // Cascada: el archivo se persistió con la entrega (consulta real, sin navegar lazy).
            Entrega entregaRec = entregas.findById(entregaId).orElseThrow();
            assertThat(entregaRec.getEstado()).isEqualTo(EstadoEntrega.PENDIENTE);
            assertThat(archivos.findAllByEntrega_IdOrderByOrdenAsc(entregaId))
                    .singleElement()
                    .extracting(ArchivoEntrega::getNombreOriginal)
                    .isEqualTo("informe.pdf");
            assertThat(fragmentos.countByEntregaId(entregaId)).isEqualTo(1);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void lotesYEntregasDeUnTenantSonInvisiblesParaOtro() {
        UUID tenantA = nuevaInstitucion("Inst A");
        UUID tenantB = nuevaInstitucion("Inst B");
        UUID docenteA = nuevoDocente(tenantA, "doc@inst-a.test");
        UUID materiaA = nuevaMateria(tenantA, docenteA);
        UUID rubricaA = nuevaRubrica(tenantA, docenteA, materiaA);

        UUID loteId;
        TenantContext.set(tenantA);
        try {
            Lote lote = new Lote();
            lote.setDocenteId(docenteA);
            lote.setMateriaId(materiaA);
            lote.setRubricaId(rubricaA);
            lote.setNombre("Lote A");
            loteId = lotes.save(lote).getId();
        } finally {
            TenantContext.clear();
        }

        TenantContext.set(tenantB);
        assertThat(lotes.findById(loteId)).isEmpty();
        assertThat(lotes.findAll()).extracting(Lote::getId).doesNotContain(loteId);
    }

    private float[] nuevoEmbedding() {
        float[] emb = new float[1024];
        for (int i = 0; i < emb.length; i++) {
            emb[i] = (i % 7) * 0.125f;
        }
        return emb;
    }

    // --- helpers (mismo patrón que MultiTenantIsolationTest) ---

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

    private UUID nuevaRubrica(UUID tenantId, UUID docenteId, UUID materiaId) {
        TenantContext.set(tenantId);
        try {
            Rubrica rubrica = new Rubrica();
            rubrica.setDocenteId(docenteId);
            rubrica.setMateriaId(materiaId);
            rubrica.setNombre("Rúbrica");
            rubrica.setPuntajeTotal(new BigDecimal("100.00"));
            rubrica.setModoTotal(ModoTotal.SUMA);
            return rubricas.save(rubrica).getId();
        } finally {
            TenantContext.clear();
        }
    }
}
