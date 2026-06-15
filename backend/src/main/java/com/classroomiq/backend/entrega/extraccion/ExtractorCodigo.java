package com.classroomiq.backend.entrega.extraccion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;

/**
 * Extrae texto de un ZIP de código: descomprime, filtra los archivos relevantes por extensión,
 * ignora dependencias y artefactos (node_modules, .git, target, …), y produce un segmento por
 * archivo de código. Los notebooks (.ipynb) se delegan al {@link NotebookParser}.
 *
 * <p>Aplica guardas anti zip-bomb: límite de entradas, de tamaño por archivo y de tamaño total
 * descomprimido.
 */
@Component
class ExtractorCodigo implements ExtractorArchivo {

    private static final Set<String> DIRECTORIOS_IGNORADOS = Set.of(
            "node_modules", ".git", "__pycache__", "target", "build", "dist",
            ".venv", "venv", ".idea", ".gradle", "vendor", "bin", "obj", ".mvn");

    private static final int MAX_ENTRADAS = 10_000;
    private static final long MAX_BYTES_ARCHIVO = 16L * 1024 * 1024;
    private static final long MAX_BYTES_TOTAL = 128L * 1024 * 1024;

    private final NotebookParser notebookParser;

    ExtractorCodigo(NotebookParser notebookParser) {
        this.notebookParser = notebookParser;
    }

    @Override
    public boolean soporta(String nombreArchivo) {
        return nombreArchivo.toLowerCase().endsWith(".zip");
    }

    @Override
    public List<SegmentoTexto> extraer(Path ruta, String nombreLogico) {
        List<SegmentoTexto> segmentos = new ArrayList<>();
        int entradas = 0;
        long bytesTotal = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(ruta))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                if (++entradas > MAX_ENTRADAS) {
                    throw new ExtraccionException("El ZIP '" + nombreLogico + "' tiene demasiadas entradas");
                }
                if (entrada.isDirectory() || ignorada(entrada.getName())) {
                    continue;
                }
                boolean notebook = LenguajeDetector.esNotebook(entrada.getName());
                if (!notebook && !LenguajeDetector.esCodigo(entrada.getName())) {
                    continue;
                }
                byte[] contenido = leerEntrada(zip, nombreLogico);
                bytesTotal += contenido.length;
                if (bytesTotal > MAX_BYTES_TOTAL) {
                    throw new ExtraccionException("El ZIP '" + nombreLogico + "' supera el tamaño máximo descomprimido");
                }
                if (notebook) {
                    segmentos.addAll(notebookParser.parsear(contenido, entrada.getName()));
                } else {
                    segmentos.add(segmentoCodigo(entrada.getName(), contenido));
                }
            }
        } catch (IOException e) {
            throw new ExtraccionException("No se pudo leer el ZIP '" + nombreLogico + "'", e);
        }
        return segmentos;
    }

    private SegmentoTexto segmentoCodigo(String nombre, byte[] contenido) {
        String texto = new String(contenido, StandardCharsets.UTF_8);
        int lineas = (int) Math.max(1, texto.lines().count());
        return SegmentoTexto.de(nombre, null, LenguajeDetector.lenguaje(nombre).orElse(null),
                1, lineas, texto.strip());
    }

    private boolean ignorada(String rutaEntrada) {
        for (String segmento : rutaEntrada.split("/")) {
            if (DIRECTORIOS_IGNORADOS.contains(segmento)) {
                return true;
            }
        }
        return false;
    }

    private byte[] leerEntrada(ZipInputStream zip, String nombreLogico) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int leido;
        while ((leido = zip.read(buffer)) != -1) {
            total += leido;
            if (total > MAX_BYTES_ARCHIVO) {
                throw new ExtraccionException("Un archivo del ZIP '" + nombreLogico + "' es demasiado grande");
            }
            out.write(buffer, 0, leido);
        }
        return out.toByteArray();
    }
}
