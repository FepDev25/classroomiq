package com.classroomiq.backend;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.FragmentoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.domain.RolArchivo;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.procesamiento.ProcesadorEntrega;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.entrega.storage.StorageService;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.provider.embeddings.EmbeddingProvider;
import com.classroomiq.backend.rubrica.domain.ModoTotal;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * End-to-end del pipeline de indexación (Hito 4): ZIP en disco → extracción → chunks → embeddings
 * → fragmentos en pgvector. Usa un {@link EmbeddingProvider} stub (dim 1024) para no depender de
 * Ollama; verifica round-trip del vector, reproceso idempotente y aislamiento por tenant.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, IndexacionTest.StubEmbeddings.class})
class IndexacionTest {

    @TestConfiguration
    static class StubEmbeddings {
        @Bean
        EmbeddingProvider embeddingProvider() {
            return new EmbeddingProvider() {
                @Override
                public List<float[]> embed(List<String> textos) {
                    return textos.stream().map(this::vector).toList();
                }

                @Override
                public int dimension() {
                    return 1024;
                }

                @Override
                public String modelo() {
                    return "stub";
                }

                private float[] vector(String texto) {
                    Random r = new Random(texto.hashCode());
                    float[] v = new float[1024];
                    double suma = 0;
                    for (int i = 0; i < v.length; i++) {
                        v[i] = r.nextFloat();
                        suma += v[i] * v[i];
                    }
                    double norma = Math.sqrt(suma);
                    for (int i = 0; i < v.length; i++) {
                        v[i] /= norma;
                    }
                    return v;
                }
            };
        }
    }

    static Path storageDir;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws IOException {
        storageDir = Files.createTempDirectory("classroomiq-index-test");
        registry.add("app.storage.base-path", storageDir::toString);
        // Excluye el proveedor Ollama real; usamos el stub.
        registry.add("app.embeddings.provider", () -> "stub");
    }

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
    private StorageService storage;
    @Autowired
    private ProcesadorEntrega procesador;

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    @Test
    void indexaEntregaCodigoYPersisteFragmentosVectoriales() throws Exception {
        UUID tenant = nuevaInstitucion("Inst Index");
        UUID docente = nuevoDocente(tenant, "doc@index.test");
        UUID materia = nuevaMateria(tenant, docente);
        UUID rubrica = nuevaRubrica(tenant, docente, materia);

        TenantContext.set(tenant);
        try {
            Lote lote = new Lote();
            lote.setDocenteId(docente);
            lote.setMateriaId(materia);
            lote.setRubricaId(rubrica);
            lote.setNombre("Lote Index");
            UUID loteId = lotes.save(lote).getId();

            Entrega entrega = new Entrega();
            entrega.setLoteId(loteId);
            entrega.setDocenteId(docente);
            entrega.setMateriaId(materia);
            entrega.setIdentificadorEstudiante("g1");
            entrega.setTipo(TipoEntrega.CODIGO);

            String rel = tenant + "/" + materia + "/" + loteId + "/proyecto.zip";
            escribirZip(storage.resolver(rel));
            ArchivoEntrega archivo = new ArchivoEntrega();
            archivo.setNombreOriginal("proyecto.zip");
            archivo.setRutaRelativa(rel);
            archivo.setTamanoBytes(Files.size(storage.resolver(rel)));
            archivo.setRol(RolArchivo.CODIGO);
            archivo.setOrden(0);
            entrega.addArchivo(archivo);
            Entrega persistida = entregas.save(entrega);

            int generados = procesador.indexar(persistida, persistida.getArchivos());
            assertThat(generados).isPositive();
            assertThat(fragmentos.countByEntregaId(persistida.getId())).isEqualTo(generados);

            List<FragmentoEntrega> frags = fragmentos.findAllByEntregaIdOrderByOrdenAsc(persistida.getId());
            assertThat(frags).allSatisfy(f -> {
                assertThat(f.getEmbedding()).hasSize(1024);
                assertThat(f.getContenido()).isNotBlank();
                assertThat(f.getLoteId()).isEqualTo(loteId);
            });
            assertThat(frags).anySatisfy(f -> assertThat(f.getContenido()).contains("def calcular"));

            // Reproceso idempotente: no duplica fragmentos.
            int reproceso = procesador.indexar(persistida, persistida.getArchivos());
            assertThat(fragmentos.countByEntregaId(persistida.getId())).isEqualTo(reproceso);

            // Aislamiento: otro tenant no ve los fragmentos.
            UUID otro = nuevaInstitucion("Otro");
            TenantContext.set(otro);
            assertThat(fragmentos.countByEntregaId(persistida.getId())).isZero();
        } finally {
            TenantContext.clear();
        }
    }

    private void escribirZip(Path destino) throws IOException {
        Files.createDirectories(destino.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destino))) {
            zip.putNextEntry(new ZipEntry("src/calculadora.py"));
            String codigo = "def calcular(a, b):\n    return a + b\n\nprint(calcular(2, 3))\n";
            zip.write(codigo.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    // --- helpers ---

    private UUID nuevaInstitucion(String nombre) {
        TenantContext.clear();
        Institucion i = new Institucion();
        i.setNombre(nombre);
        return instituciones.save(i).getId();
    }

    private UUID nuevoDocente(UUID tenant, String email) {
        TenantContext.set(tenant);
        try {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setPasswordHash("x");
            u.setNombre("Docente");
            u.setRol(Rol.DOCENTE);
            return usuarios.save(u).getId();
        } finally {
            TenantContext.clear();
        }
    }

    private UUID nuevaMateria(UUID tenant, UUID docente) {
        TenantContext.set(tenant);
        try {
            Materia m = new Materia();
            m.setDocenteId(docente);
            m.setNombre("Programación");
            return materias.save(m).getId();
        } finally {
            TenantContext.clear();
        }
    }

    private UUID nuevaRubrica(UUID tenant, UUID docente, UUID materia) {
        TenantContext.set(tenant);
        try {
            Rubrica r = new Rubrica();
            r.setDocenteId(docente);
            r.setMateriaId(materia);
            r.setNombre("Rúbrica");
            r.setPuntajeTotal(new BigDecimal("100.00"));
            r.setModoTotal(ModoTotal.SUMA);
            return rubricas.save(r).getId();
        } finally {
            TenantContext.clear();
        }
    }
}
