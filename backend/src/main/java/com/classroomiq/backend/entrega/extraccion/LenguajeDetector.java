package com.classroomiq.backend.entrega.extraccion;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Detección de lenguaje de código por extensión de archivo. El conjunto de extensiones conocidas
 * define además qué archivos del ZIP se consideran código relevante.
 */
final class LenguajeDetector {

    private static final Map<String, String> POR_EXTENSION = Map.ofEntries(
            Map.entry("py", "Python"),
            Map.entry("java", "Java"),
            Map.entry("kt", "Kotlin"),
            Map.entry("js", "JavaScript"),
            Map.entry("jsx", "JavaScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("tsx", "TypeScript"),
            Map.entry("go", "Go"),
            Map.entry("c", "C"),
            Map.entry("h", "C"),
            Map.entry("cpp", "C++"),
            Map.entry("cc", "C++"),
            Map.entry("hpp", "C++"),
            Map.entry("cs", "C#"),
            Map.entry("rb", "Ruby"),
            Map.entry("rs", "Rust"),
            Map.entry("php", "PHP"),
            Map.entry("swift", "Swift"),
            Map.entry("r", "R"),
            Map.entry("scala", "Scala"),
            Map.entry("sql", "SQL"),
            Map.entry("sh", "Shell"),
            Map.entry("m", "MATLAB"));

    /**
     * Archivos de texto plano relevantes en una entrega de código (READMEs, documentación). No son
     * un lenguaje de programación, pero su contenido se evalúa (ej. el criterio "Documentación").
     */
    private static final Set<String> EXTENSIONES_TEXTO = Set.of("md", "markdown", "txt", "rst");

    private LenguajeDetector() {
    }

    static boolean esCodigo(String nombreArchivo) {
        return POR_EXTENSION.containsKey(extension(nombreArchivo));
    }

    static boolean esTexto(String nombreArchivo) {
        return EXTENSIONES_TEXTO.contains(extension(nombreArchivo));
    }

    static boolean esNotebook(String nombreArchivo) {
        return "ipynb".equals(extension(nombreArchivo));
    }

    static Optional<String> lenguaje(String nombreArchivo) {
        return Optional.ofNullable(POR_EXTENSION.get(extension(nombreArchivo)));
    }

    private static String extension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        return punto < 0 ? "" : nombreArchivo.substring(punto + 1).toLowerCase();
    }
}
