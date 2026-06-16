package com.classroomiq.backend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.domain.RolArchivo;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.procesamiento.ProcesadorEntrega;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.entrega.storage.StorageService;
import com.classroomiq.backend.evaluacion.domain.Evaluacion;
import com.classroomiq.backend.evaluacion.domain.EvaluacionCriterio;
import com.classroomiq.backend.evaluacion.motor.MotorEvaluacion;
import com.classroomiq.backend.evaluacion.repository.EvaluacionCriterioRepository;
import com.classroomiq.backend.institucion.domain.Institucion;
import com.classroomiq.backend.institucion.repository.InstitucionRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.provider.llm.AnthropicLlmProvider;
import com.classroomiq.backend.provider.llm.LlmProvider;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.LlmSolicitud;
import com.classroomiq.backend.provider.llm.ModeloTier;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.ModoTotal;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.domain.TipoPuntaje;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validación del motor contra el ground truth (Fase 4, Hito 6). A diferencia del resto de la suite,
 * usa el LLM (Anthropic) y los embeddings (Ollama) <strong>reales</strong>: por eso es opt-in y solo
 * corre con la variable de entorno {@code VALIDACION_GROUNDTRUTH=true} (y requiere Ollama vivo y
 * {@code ANTHROPIC_API_KEY} en el .env). {@code ./mvnw test} normal la salta.
 *
 * <p>Corre el pipeline completo (extracción → chunking → embeddings → evaluación por criterio) sobre
 * las entregas ficticias de {@code data/ground-truth/} y compara el nivel/puntaje sugerido con
 * {@code etiquetas-esperadas.json} (tolerancia ±1 nivel). Expone dos vistas del enfoque acordado:
 * <ul>
 *   <li>{@link #imprimeReporteDeValidacion()} — el "harness": imprime el detalle criterio por criterio
 *       (esperado vs obtenido) y los agregados, sin aserción dura. Para iterar el prompt de H3.</li>
 *   <li>{@link #laValidacionSuperaElUmbral()} — el "check": asegura que el acierto de nivel supere un
 *       umbral ({@code VALIDACION_UMBRAL}, default 0.75).</li>
 * </ul>
 * Ambas comparten una <strong>única</strong> corrida del pipeline (@BeforeAll) para no duplicar costo.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, GroundTruthValidacionTest.ContadorTokens.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "VALIDACION_GROUNDTRUTH", matches = "(?i)1|true|yes")
class GroundTruthValidacionTest {

    private static final int TOLERANCIA_NIVEL = 1;

    // Tarifas de Sonnet 4.6 (USD por millón de tokens): entrada 3, salida 15. El thinking del tier
    // potente se factura como salida. Si cambia el modelo del motor, actualizar estas tarifas.
    private static final double USD_POR_M_INPUT = 3.0;
    private static final double USD_POR_M_OUTPUT = 15.0;

    /**
     * Envuelve el {@link LlmProvider} real para contar los tokens consumidos por la corrida y poder
     * reportar el costo exacto (no estimado). Es el mismo dato que el portal admin de Fase 6
     * acumulará por docente/institución; aquí se usa a nivel de harness.
     */
    @TestConfiguration
    static class ContadorTokens {
        static final LongAdder inputTokens = new LongAdder();
        static final LongAdder outputTokens = new LongAdder();
        static final LongAdder llamadas = new LongAdder();

        static void reset() {
            inputTokens.reset();
            outputTokens.reset();
            llamadas.reset();
        }

        @Bean
        @Primary
        LlmProvider llmProviderContador(AnthropicLlmProvider real) {
            return new LlmProvider() {
                @Override
                public LlmResultado generar(LlmSolicitud solicitud) {
                    LlmResultado resultado = real.generar(solicitud);
                    llamadas.increment();
                    if (resultado.uso() != null) {
                        inputTokens.add(resultado.uso().inputTokens());
                        outputTokens.add(resultado.uso().outputTokens());
                    }
                    return resultado;
                }

                @Override
                public String modelo(ModeloTier tier) {
                    return real.modelo(tier);
                }
            };
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.storage.base-path", () -> {
            try {
                return Files.createTempDirectory("classroomiq-gt").toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Autowired private InstitucionRepository instituciones;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MateriaRepository materias;
    @Autowired private RubricaRepository rubricas;
    @Autowired private LoteRepository lotes;
    @Autowired private EntregaRepository entregas;
    @Autowired private EvaluacionCriterioRepository evaluacionCriterios;
    @Autowired private StorageService storage;
    @Autowired private ProcesadorEntrega procesador;
    @Autowired private MotorEvaluacion motor;
    @Autowired private ObjectMapper json;

    private UUID tenantId;
    private UUID docenteId;
    private final List<ResultadoCriterio> resultados = new ArrayList<>();

    @BeforeAll
    void ejecutarPipeline() throws Exception {
        Path raiz = raizGroundTruth();
        ContadorTokens.reset();
        TenantContext.clear();
        Institucion inst = new Institucion();
        inst.setNombre("Validación GT");
        tenantId = instituciones.save(inst).getId();
        docenteId = crearDocente();

        for (Dataset ds : datasets(raiz)) {
            TenantContext.set(tenantId);
            try {
                evaluarDataset(ds);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Test
    void imprimeReporteDeValidacion() {
        StringBuilder sb = new StringBuilder("\n===== VALIDACIÓN vs GROUND TRUTH (±" + TOLERANCIA_NIVEL + " nivel) =====\n");
        String datasetActual = null;
        for (ResultadoCriterio r : resultados) {
            if (!r.dataset().equals(datasetActual)) {
                datasetActual = r.dataset();
                sb.append("\n## ").append(datasetActual).append('\n');
            }
            sb.append(String.format("  [%s] %-40s esp=%-13s obt=%-13s nivelΔ=%d %s  pts esp=%s obt=%s %s%s%n",
                    r.aciertoNivel() ? "OK " : "XX ", recorta(r.criterio(), 40),
                    r.nivelEsperado(), valorONulo(r.nivelObtenido()), r.distancia(),
                    r.aciertoNivel() ? "✓" : "✗",
                    r.puntajeEsperado(), valorONulo(r.puntajeObtenido()),
                    r.puntajeEnRangoEsperado() ? "✓" : "✗",
                    r.advertencia() == null ? "" : "  ⚠ " + recorta(r.advertencia(), 80)));
        }
        sb.append('\n').append(resumen());
        sb.append('\n').append(reporteCosto());
        System.out.println(sb);

        assertThat(resultados).as("el pipeline produjo resultados").isNotEmpty();
    }

    /** Tokens reales consumidos y costo calculado con las tarifas de Sonnet 4.6. */
    private String reporteCosto() {
        long in = ContadorTokens.inputTokens.sum();
        long out = ContadorTokens.outputTokens.sum();
        long n = ContadorTokens.llamadas.sum();
        double costoIn = in / 1_000_000.0 * USD_POR_M_INPUT;
        double costoOut = out / 1_000_000.0 * USD_POR_M_OUTPUT;
        double total = costoIn + costoOut;
        long entregas = resultados.stream().map(ResultadoCriterio::dataset).distinct().count();
        return String.format(Locale.ROOT,
                "COSTO (Sonnet 4.6, effort actual): %d llamadas al LLM | entrada %,d tok ($%.4f) + salida %,d tok ($%.4f)%n"
                        + "  TOTAL $%.4f  |  por criterio $%.4f  |  por entrega $%.4f",
                n, in, costoIn, out, costoOut,
                total, n == 0 ? 0 : total / n, entregas == 0 ? 0 : total / entregas);
    }

    @Test
    void laValidacionSuperaElUmbral() {
        double umbral = Double.parseDouble(Optional.ofNullable(System.getenv("VALIDACION_UMBRAL")).orElse("0.75"));
        long total = resultados.size();
        long aciertos = resultados.stream().filter(ResultadoCriterio::aciertoNivel).count();
        double ratio = total == 0 ? 0 : (double) aciertos / total;
        System.out.printf("%nAcierto de nivel (±%d): %d/%d = %.0f%% (umbral %.0f%%)%n",
                TOLERANCIA_NIVEL, aciertos, total, ratio * 100, umbral * 100);
        assertThat(ratio)
                .as("proporción de criterios con nivel dentro de ±%d (umbral %.2f)", TOLERANCIA_NIVEL, umbral)
                .isGreaterThanOrEqualTo(umbral);
    }

    private String resumen() {
        long total = resultados.size();
        long aciertoNivel = resultados.stream().filter(ResultadoCriterio::aciertoNivel).count();
        long puntajeOk = resultados.stream().filter(ResultadoCriterio::puntajeEnRangoEsperado).count();
        long exacto = resultados.stream().filter(r -> r.distancia() == 0).count();
        return String.format(Locale.ROOT,
                "RESUMEN: %d criterios | nivel ±%d: %d (%.0f%%) | nivel exacto: %d (%.0f%%) | puntaje en rango esperado: %d (%.0f%%)",
                total, TOLERANCIA_NIVEL, aciertoNivel, pct(aciertoNivel, total),
                exacto, pct(exacto, total), puntajeOk, pct(puntajeOk, total));
    }

    // --- pipeline por dataset ---

    private void evaluarDataset(Dataset ds) throws Exception {
        GtRubrica gt = json.readValue(Files.readAllBytes(ds.rubricaPath()), GtRubrica.class);
        Etiquetas etiquetas = json.readValue(Files.readAllBytes(ds.etiquetasPath()), Etiquetas.class);

        UUID materiaId = crearMateria(gt.materia() != null ? gt.materia() : ds.nombre());
        Rubrica rubrica = rubricas.save(construirRubrica(gt, materiaId));

        for (EntregaEsperada esperada : etiquetas.entregas()) {
            byte[] zip = comprimir(ds.baseDir().resolve(esperada.archivo()));
            UUID entregaId = indexarEntrega(materiaId, rubrica.getId(), esperada.archivo(), zip);

            Evaluacion eval = motor.evaluar(entregaId);
            recolectar(ds.nombre(), esperada, eval, rubrica);
        }
    }

    private UUID indexarEntrega(UUID materiaId, UUID rubricaId, String alias, byte[] zipBytes) throws IOException {
        Lote lote = new Lote();
        lote.setDocenteId(docenteId);
        lote.setMateriaId(materiaId);
        lote.setRubricaId(rubricaId);
        lote.setNombre("GT " + alias);
        UUID loteId = lotes.save(lote).getId();

        Entrega entrega = new Entrega();
        entrega.setLoteId(loteId);
        entrega.setDocenteId(docenteId);
        entrega.setMateriaId(materiaId);
        entrega.setIdentificadorEstudiante(alias);
        entrega.setTipo(TipoEntrega.CODIGO);

        String rel = tenantId + "/" + materiaId + "/" + loteId + "/entrega.zip";
        Path destino = storage.resolver(rel);
        Files.createDirectories(destino.getParent());
        Files.write(destino, zipBytes);

        ArchivoEntrega archivo = new ArchivoEntrega();
        archivo.setNombreOriginal("entrega.zip");
        archivo.setRutaRelativa(rel);
        archivo.setTamanoBytes((long) zipBytes.length);
        archivo.setRol(RolArchivo.CODIGO);
        archivo.setOrden(0);
        entrega.addArchivo(archivo);
        Entrega persistida = entregas.save(entrega);

        procesador.indexar(persistida, persistida.getArchivos());
        persistida.setEstado(EstadoEntrega.LISTO);
        return entregas.save(persistida).getId();
    }

    private void recolectar(String dataset, EntregaEsperada esperada, Evaluacion eval, Rubrica rubrica) {
        Map<UUID, Criterio> porId = rubrica.getCriterios().stream()
                .collect(java.util.stream.Collectors.toMap(Criterio::getId, c -> c));
        List<EvaluacionCriterio> ecs = evaluacionCriterios.findAllByEvaluacionIdOrderByOrdenAsc(eval.getId());

        for (CriterioEsperado ce : esperada.criterios()) {
            EvaluacionCriterio ec = ecs.stream()
                    .filter(e -> nombre(porId, e.getCriterioId()).equalsIgnoreCase(ce.criterio()))
                    .findFirst().orElse(null);
            Criterio criterio = ec == null ? null : porId.get(ec.getCriterioId());

            String nivelObtenido = ec == null ? null : nombreNivel(criterio, ec.getNivelSugeridoId());
            int idxEsperado = indiceNivel(criterio, ce.nivelEsperado());
            int idxObtenido = ec == null ? -1 : indiceNivel(criterio, nivelObtenido);
            int distancia = (idxEsperado < 0 || idxObtenido < 0) ? 99 : Math.abs(idxEsperado - idxObtenido);

            boolean aciertoNivel = distancia <= TOLERANCIA_NIVEL;
            BigDecimal puntajeObtenido = ec == null ? null : ec.getPuntajeSugerido();
            boolean puntajeOk = puntajeObtenido != null && enRangoEsperado(criterio, ce.nivelEsperado(), puntajeObtenido);

            resultados.add(new ResultadoCriterio(dataset + " / " + esperada.archivo(), ce.criterio(),
                    ce.nivelEsperado(), nivelObtenido, distancia, aciertoNivel,
                    ce.puntajeEsperado(), puntajeObtenido, puntajeOk,
                    ec == null ? "sin criterio" : ec.getAdvertencia()));
        }
    }

    // --- comparación de niveles/puntajes ---

    private int indiceNivel(Criterio criterio, String nombreNivel) {
        if (criterio == null || nombreNivel == null) {
            return -1;
        }
        List<NivelDesempeno> niveles = criterio.getNiveles();
        for (int i = 0; i < niveles.size(); i++) {
            if (niveles.get(i).getNombre().equalsIgnoreCase(nombreNivel.trim())) {
                return i;
            }
        }
        return -1;
    }

    private boolean enRangoEsperado(Criterio criterio, String nivelEsperado, BigDecimal puntaje) {
        if (criterio == null) {
            return false;
        }
        return criterio.getNiveles().stream()
                .filter(n -> n.getNombre().equalsIgnoreCase(nivelEsperado.trim()))
                .findFirst()
                .map(n -> puntaje.compareTo(n.getPuntajeMin()) >= 0 && puntaje.compareTo(n.getPuntajeMax()) <= 0)
                .orElse(false);
    }

    private String nombreNivel(Criterio criterio, UUID nivelId) {
        if (criterio == null || nivelId == null) {
            return null;
        }
        return criterio.getNiveles().stream().filter(n -> n.getId().equals(nivelId))
                .map(NivelDesempeno::getNombre).findFirst().orElse(null);
    }

    private String nombre(Map<UUID, Criterio> porId, UUID criterioId) {
        Criterio c = porId.get(criterioId);
        return c == null ? "" : c.getNombre();
    }

    // --- construcción de la rúbrica desde el JSON (tolera ambos esquemas) ---

    private Rubrica construirRubrica(GtRubrica gt, UUID materiaId) {
        Rubrica rubrica = new Rubrica();
        rubrica.setDocenteId(docenteId);
        rubrica.setMateriaId(materiaId);
        rubrica.setNombre(gt.nombre());
        rubrica.setDescripcion(gt.descripcion());
        rubrica.setPuntajeTotal(gt.puntajeTotal());
        rubrica.setModoTotal(ModoTotal.valueOf(gt.modoTotal().trim().toUpperCase(Locale.ROOT)));
        int ordenC = 0;
        for (GtCriterio gc : gt.criterios()) {
            Criterio c = new Criterio();
            c.setNombre(gc.nombre());
            c.setDescripcion(gc.descripcion());
            c.setPuntajeMaximo(gc.puntajeMaximo());
            c.setEvaluablePorContenido(gc.evaluablePorContenido() == null || gc.evaluablePorContenido());
            c.setOrden(ordenC++);
            int ordenN = 0;
            for (GtNivel gn : gc.niveles()) {
                NivelDesempeno n = new NivelDesempeno();
                n.setNombre(gn.nombre());
                n.setDescripcion(gn.descripcion());
                n.setOrden(ordenN++);
                TipoPuntaje tipo = TipoPuntaje.valueOf(gn.tipoPuntaje().trim().toUpperCase(Locale.ROOT)
                        .replace("BANDAPCT", "BANDA_PCT"));
                n.setTipoPuntaje(tipo);
                switch (tipo) {
                    case RANGO -> {
                        n.setPuntajeMin(gn.min());
                        n.setPuntajeMax(gn.max());
                    }
                    case FIJO -> n.setPuntajeValor(gn.valor());
                    case BANDA_PCT -> {
                        n.setPctMin(gn.pctMin());
                        n.setPctMax(gn.pctMax());
                    }
                }
                c.addNivel(n);
            }
            rubrica.addCriterio(c);
        }
        return rubrica;
    }

    // --- archivos ---

    /** Comprime en un .zip un archivo suelto o todos los archivos de un directorio (recursivo). */
    private byte[] comprimir(Path origen) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            if (Files.isDirectory(origen)) {
                try (Stream<Path> archivos = Files.walk(origen)) {
                    for (Path p : archivos.filter(Files::isRegularFile).toList()) {
                        agregar(zip, origen.relativize(p).toString(), Files.readAllBytes(p));
                    }
                }
            } else {
                agregar(zip, origen.getFileName().toString(), Files.readAllBytes(origen));
            }
        }
        return baos.toByteArray();
    }

    private void agregar(ZipOutputStream zip, String nombre, byte[] contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido);
        zip.closeEntry();
    }

    private List<Dataset> datasets(Path raiz) {
        return List.of(
                new Dataset("informe-proyecto-software",
                        raiz.resolve("../rubricas-ejemplo/informe-proyecto-software.json").normalize(),
                        raiz.resolve("informe-proyecto-software/etiquetas-esperadas.json"),
                        raiz.resolve("informe-proyecto-software")),
                new Dataset("tarea-pila-python",
                        raiz.resolve("tarea-pila-python/rubrica.json"),
                        raiz.resolve("tarea-pila-python/etiquetas-esperadas.json"),
                        raiz.resolve("tarea-pila-python")));
    }

    /** El test corre con working dir = backend/; el ground truth vive en la raíz del repo. */
    private Path raizGroundTruth() {
        Path local = Path.of("data/ground-truth");
        return Files.isDirectory(local) ? local : Path.of("../data/ground-truth");
    }

    // --- helpers de creación ---

    private UUID crearDocente() {
        TenantContext.set(tenantId);
        try {
            Usuario u = new Usuario();
            u.setEmail("gt-" + UUID.randomUUID() + "@val.test");
            u.setNombre("Docente GT");
            u.setPasswordHash("x");
            u.setRol(Rol.DOCENTE);
            u.setActivo(true);
            return usuarios.save(u).getId();
        } finally {
            TenantContext.clear();
        }
    }

    private UUID crearMateria(String nombre) {
        Materia m = new Materia();
        m.setDocenteId(docenteId);
        m.setNombre(nombre);
        return materias.save(m).getId();
    }

    private static double pct(long n, long total) {
        return total == 0 ? 0 : 100.0 * n / total;
    }

    private static String valorONulo(Object o) {
        return o == null ? "—" : o.toString();
    }

    private static String recorta(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // --- records de datos ---

    private record Dataset(String nombre, Path rubricaPath, Path etiquetasPath, Path baseDir) {
    }

    private record ResultadoCriterio(String dataset, String criterio, String nivelEsperado,
            String nivelObtenido, int distancia, boolean aciertoNivel, BigDecimal puntajeEsperado,
            BigDecimal puntajeObtenido, boolean puntajeEnRangoEsperado, String advertencia) {
    }

    record Etiquetas(String rubrica, int toleranciaNivel, List<EntregaEsperada> entregas) {
    }

    record EntregaEsperada(String archivo, String perfil, BigDecimal totalEsperado, List<CriterioEsperado> criterios) {
    }

    record CriterioEsperado(String criterio, String nivelEsperado, BigDecimal puntajeEsperado) {
    }

    record GtRubrica(String nombre, String materia, String descripcion, BigDecimal puntajeTotal,
            String modoTotal, List<GtCriterio> criterios) {
    }

    record GtCriterio(String nombre, String descripcion, BigDecimal puntajeMaximo,
            Boolean evaluablePorContenido, List<GtNivel> niveles) {
    }

    record GtNivel(String nombre, String descripcion, String tipoPuntaje,
            @JsonAlias({"min", "puntajeMin"}) BigDecimal min,
            @JsonAlias({"max", "puntajeMax"}) BigDecimal max,
            @JsonAlias({"valor", "puntajeValor"}) BigDecimal valor,
            BigDecimal pctMin, BigDecimal pctMax) {
    }
}
