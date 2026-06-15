package com.classroomiq.backend.entrega.extraccion;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Extrae texto de un PDF (Apache PDFBox), una sección por página. Limpia la numeración de página
 * y las líneas de encabezado/pie repetidas en la mayoría de las páginas (heurística de
 * header/footer recurrente).
 */
@Component
class ExtractorPdf implements ExtractorArchivo {

    @Override
    public boolean soporta(String nombreArchivo) {
        return nombreArchivo.toLowerCase().endsWith(".pdf");
    }

    @Override
    public List<SegmentoTexto> extraer(Path ruta, String nombreLogico) {
        try (PDDocument documento = Loader.loadPDF(ruta.toFile())) {
            int paginas = documento.getNumberOfPages();
            List<List<String>> lineasPorPagina = new ArrayList<>(paginas);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= paginas; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                lineasPorPagina.add(List.of(stripper.getText(documento).split("\\R")));
            }

            List<String> recurrentes = lineasRecurrentes(lineasPorPagina);
            List<SegmentoTexto> segmentos = new ArrayList<>();
            for (int i = 0; i < lineasPorPagina.size(); i++) {
                String limpio = limpiarPagina(lineasPorPagina.get(i), recurrentes);
                if (!limpio.isBlank()) {
                    segmentos.add(SegmentoTexto.de(nombreLogico, "Página " + (i + 1),
                            null, null, null, limpio));
                }
            }
            return segmentos;
        } catch (IOException e) {
            throw new ExtraccionException("No se pudo leer el PDF '" + nombreLogico + "'", e);
        }
    }

    /** Líneas que aparecen en al menos el 60% de las páginas (>=3 páginas): headers/footers. */
    private List<String> lineasRecurrentes(List<List<String>> lineasPorPagina) {
        int paginas = lineasPorPagina.size();
        if (paginas < 3) {
            return List.of();
        }
        Map<String, Integer> frecuencia = new HashMap<>();
        for (List<String> lineas : lineasPorPagina) {
            lineas.stream()
                    .map(String::trim)
                    .filter(linea -> !linea.isBlank())
                    .distinct()
                    .forEach(linea -> frecuencia.merge(linea, 1, Integer::sum));
        }
        int umbral = (int) Math.ceil(paginas * 0.6);
        return frecuencia.entrySet().stream()
                .filter(e -> e.getValue() >= umbral)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String limpiarPagina(List<String> lineas, List<String> recurrentes) {
        StringBuilder sb = new StringBuilder();
        for (String linea : lineas) {
            String t = linea.trim();
            if (t.isEmpty() || esNumeroDePagina(t) || recurrentes.contains(t)) {
                continue;
            }
            sb.append(t).append('\n');
        }
        return sb.toString().strip();
    }

    private boolean esNumeroDePagina(String linea) {
        // "12", "- 12 -", "Página 12", "12 / 30"
        return linea.matches("(?i)[-\\s]*(página|pag\\.?)?\\s*\\d+\\s*([/-]\\s*\\d+)?\\s*[-]*");
    }
}
