package com.classroomiq.backend.entrega.extraccion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parsea notebooks Jupyter (.ipynb): extrae una sección por celda (código o markdown), uniendo
 * las líneas de {@code source}. El lenguaje de las celdas de código se toma de
 * {@code metadata.language_info.name}. Se usa dentro del ZIP de código.
 */
@Component
class NotebookParser {

    private final ObjectMapper json;

    NotebookParser(ObjectMapper json) {
        this.json = json;
    }

    List<SegmentoTexto> parsear(byte[] contenido, String origen) {
        JsonNode raiz;
        try {
            raiz = json.readTree(contenido);
        } catch (IOException e) {
            throw new ExtraccionException("Notebook inválido: '" + origen + "'", e);
        }
        String lenguaje = raiz.path("metadata").path("language_info").path("name").asText(null);

        List<SegmentoTexto> segmentos = new ArrayList<>();
        JsonNode celdas = raiz.path("cells");
        for (int i = 0; i < celdas.size(); i++) {
            JsonNode celda = celdas.get(i);
            String tipo = celda.path("cell_type").asText("");
            String fuente = unirSource(celda.path("source"));
            if (fuente.isBlank()) {
                continue;
            }
            boolean esCodigo = "code".equals(tipo);
            int lineas = (int) fuente.lines().count();
            segmentos.add(SegmentoTexto.de(origen,
                    "Celda " + (i + 1) + " (" + tipo + ")",
                    esCodigo ? lenguaje : null,
                    1, lineas, fuente));
        }
        return segmentos;
    }

    /** {@code source} puede ser un array de strings o un único string. */
    private String unirSource(JsonNode source) {
        if (source.isArray()) {
            StringBuilder sb = new StringBuilder();
            source.forEach(n -> sb.append(n.asText()));
            return sb.toString().strip();
        }
        return source.asText("").strip();
    }
}
