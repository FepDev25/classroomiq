package com.classroomiq.backend.entrega.extraccion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

/**
 * Extrae texto de un DOCX (Apache POI), agrupando los párrafos en secciones según los estilos de
 * título (Heading/Título). Si el documento no tiene títulos, devuelve un único segmento.
 */
@Component
class ExtractorDocx implements ExtractorArchivo {

    @Override
    public boolean soporta(String nombreArchivo) {
        return nombreArchivo.toLowerCase().endsWith(".docx");
    }

    @Override
    public List<SegmentoTexto> extraer(Path ruta, String nombreLogico) {
        try (InputStream in = Files.newInputStream(ruta); XWPFDocument doc = new XWPFDocument(in)) {
            List<SegmentoTexto> segmentos = new ArrayList<>();
            String seccionActual = null;
            StringBuilder cuerpo = new StringBuilder();

            for (XWPFParagraph parrafo : doc.getParagraphs()) {
                String texto = parrafo.getText() == null ? "" : parrafo.getText().strip();
                if (texto.isEmpty()) {
                    continue;
                }
                if (esTitulo(parrafo)) {
                    volcar(segmentos, nombreLogico, seccionActual, cuerpo);
                    seccionActual = texto;
                    cuerpo.setLength(0);
                } else {
                    cuerpo.append(texto).append('\n');
                }
            }
            volcar(segmentos, nombreLogico, seccionActual, cuerpo);
            return segmentos;
        } catch (IOException e) {
            throw new ExtraccionException("No se pudo leer el DOCX '" + nombreLogico + "'", e);
        }
    }

    private boolean esTitulo(XWPFParagraph parrafo) {
        String estilo = parrafo.getStyleID();
        if (estilo == null) {
            return false;
        }
        String e = estilo.toLowerCase();
        return e.startsWith("heading") || e.startsWith("titulo") || e.startsWith("ttulo")
                || e.startsWith("title");
    }

    private void volcar(List<SegmentoTexto> segmentos, String origen, String seccion, StringBuilder cuerpo) {
        String contenido = cuerpo.toString().strip();
        if (!contenido.isBlank()) {
            segmentos.add(SegmentoTexto.de(origen, seccion, null, null, null, contenido));
        }
    }
}
