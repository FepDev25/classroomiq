package com.classroomiq.backend.entrega.extraccion;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.storage.StorageProperties;
import com.classroomiq.backend.entrega.storage.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests de los extractores de texto y del orquestador, con archivos PDF/DOCX/ZIP generados en
 * vivo (sin fixtures binarios). No usan contexto Spring.
 */
class ExtraccionTest {

    private static final String NOTEBOOK = """
            {"metadata":{"language_info":{"name":"python"}},
             "cells":[
               {"cell_type":"markdown","source":["# Análisis\\n","Resumen del trabajo"]},
               {"cell_type":"code","source":"import pandas as pd\\nprint('hola')"}
             ]}
            """;

    @Test
    void pdfQuitaEncabezadosRepetidosYNumeracion(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("informe.pdf");
        crearPdf(pdf, 4);

        List<SegmentoTexto> segmentos = new ExtractorPdf().extraer(pdf, "informe.pdf");

        assertThat(segmentos).hasSize(4);
        assertThat(segmentos).allSatisfy(s -> {
            assertThat(s.contenido()).doesNotContain("Encabezado Repetido");
            assertThat(s.origen()).isEqualTo("informe.pdf");
        });
        assertThat(segmentos.get(0).contenido()).contains("Cuerpo unico pagina 1");
        assertThat(segmentos.get(0).seccion()).isEqualTo("Página 1");
    }

    @Test
    void docxSeparaPorSecciones(@TempDir Path dir) throws Exception {
        Path docx = dir.resolve("informe.docx");
        crearDocx(docx);

        List<SegmentoTexto> segmentos = new ExtractorDocx().extraer(docx, "informe.docx");

        assertThat(segmentos).hasSize(2);
        assertThat(segmentos.get(0).seccion()).isEqualTo("Introducción");
        assertThat(segmentos.get(0).contenido()).contains("Este es el cuerpo introductorio");
        assertThat(segmentos.get(1).seccion()).isEqualTo("Métodos");
    }

    @Test
    void zipFiltraCodigoIgnoraDependenciasYParseaNotebook(@TempDir Path dir) throws Exception {
        Path zip = dir.resolve("proyecto.zip");
        crearZip(zip);

        ExtractorCodigo extractor = new ExtractorCodigo(new NotebookParser(new ObjectMapper()));
        List<SegmentoTexto> segmentos = extractor.extraer(zip, "proyecto.zip");

        // main.py incluido con lenguaje detectado.
        assertThat(segmentos).anySatisfy(s -> {
            assertThat(s.origen()).contains("main.py");
            assertThat(s.lenguaje()).isEqualTo("Python");
            assertThat(s.contenido()).contains("def main");
        });
        // Notebook: 2 celdas.
        assertThat(segmentos).filteredOn(s -> s.origen().contains("analysis.ipynb")).hasSize(2);
        // README.md se extrae como texto plano (prosa, sin lenguaje) para evaluar la documentación.
        assertThat(segmentos).anySatisfy(s -> {
            assertThat(s.origen()).contains("README.md");
            assertThat(s.lenguaje()).isNull();
            assertThat(s.contenido()).contains("# Proyecto");
        });
        // Dependencias en node_modules ignoradas.
        assertThat(segmentos).noneSatisfy(s -> assertThat(s.origen()).contains("node_modules"));
    }

    @Test
    void orquestadorProcesaEntregaMixtaEtiquetandoElArchivo(@TempDir Path dir) throws Exception {
        StorageService storage = new StorageService(new StorageProperties(dir.toString()));
        crearPdf(dir.resolve("a.pdf"), 1);
        crearZip(dir.resolve("b.zip"));

        ArchivoEntrega pdf = archivo("informe.pdf", "a.pdf");
        ArchivoEntrega zip = archivo("proyecto.zip", "b.zip");

        ServicioExtraccion servicio = new ServicioExtraccion(
                List.of(new ExtractorPdf(), new ExtractorDocx(),
                        new ExtractorCodigo(new NotebookParser(new ObjectMapper()))),
                storage);
        List<SegmentoTexto> segmentos = servicio.extraer(List.of(pdf, zip));

        assertThat(segmentos).anyMatch(s -> pdf.getId().equals(s.archivoId()));
        assertThat(segmentos).anyMatch(s -> zip.getId().equals(s.archivoId()));
        assertThat(segmentos).allSatisfy(s -> assertThat(s.archivoId()).isNotNull());
    }

    // --- generadores de archivos ---

    private void crearPdf(Path destino, int paginas) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int p = 1; p <= paginas; p++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(50, 750);
                    cs.showText("Encabezado Repetido");
                    cs.newLineAtOffset(0, -20);
                    cs.showText("Cuerpo unico pagina " + p);
                    cs.newLineAtOffset(0, -20);
                    cs.showText(String.valueOf(p));
                    cs.endText();
                }
            }
            doc.save(destino.toFile());
        }
    }

    private void crearDocx(Path destino) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(); OutputStream out = Files.newOutputStream(destino)) {
            titulo(doc, "Introducción");
            parrafo(doc, "Este es el cuerpo introductorio del informe.");
            titulo(doc, "Métodos");
            parrafo(doc, "Detalle de los métodos aplicados.");
            doc.write(out);
        }
    }

    private void titulo(XWPFDocument doc, String texto) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading1");
        p.createRun().setText(texto);
    }

    private void parrafo(XWPFDocument doc, String texto) {
        doc.createParagraph().createRun().setText(texto);
    }

    private void crearZip(Path destino) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destino))) {
            escribir(zip, "src/main.py", "def main():\n    print('hola')\n");
            escribir(zip, "analysis.ipynb", NOTEBOOK);
            escribir(zip, "node_modules/lib.js", "module.exports = 1;");
            escribir(zip, "README.md", "# Proyecto");
        }
    }

    private void escribir(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private ArchivoEntrega archivo(String nombreOriginal, String rutaRelativa) {
        ArchivoEntrega a = new ArchivoEntrega();
        a.setId(UUID.randomUUID());
        a.setNombreOriginal(nombreOriginal);
        a.setRutaRelativa(rutaRelativa);
        return a;
    }
}
