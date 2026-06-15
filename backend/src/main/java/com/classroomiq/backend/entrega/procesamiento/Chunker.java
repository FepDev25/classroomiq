package com.classroomiq.backend.entrega.procesamiento;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.classroomiq.backend.entrega.extraccion.SegmentoTexto;

/**
 * Re-divide los segmentos extraídos en chunks acotados por presupuesto de caracteres, preservando
 * la procedencia (origen/sección/líneas) para el resaltado posterior.
 *
 * <p>Estrategia escalonada acordada: un segmento que cabe en el presupuesto pasa tal cual; la
 * <strong>prosa</strong> (PDF/DOCX) se divide por párrafos; el <strong>código</strong> (lenguaje
 * detectado) se divide por líneas manteniendo el rango de líneas. En ambos casos se aplica solape
 * y se garantiza progreso (cada chunk avanza al menos una unidad).
 */
@Component
public class Chunker {

    private final ChunkingProperties props;

    public Chunker(ChunkingProperties props) {
        this.props = props;
    }

    public List<SegmentoTexto> dividir(List<SegmentoTexto> segmentos) {
        List<SegmentoTexto> resultado = new ArrayList<>();
        for (SegmentoTexto seg : segmentos) {
            if (seg.contenido().length() <= props.maxChars()) {
                resultado.add(seg);
            } else if (seg.lenguaje() != null) {
                resultado.addAll(dividirCodigo(seg));
            } else {
                resultado.addAll(dividirProsa(seg));
            }
        }
        return resultado;
    }

    /** Código: empaqueta líneas hasta el presupuesto, conservando el rango de líneas y con solape. */
    private List<SegmentoTexto> dividirCodigo(SegmentoTexto seg) {
        String[] lineas = seg.contenido().split("\n", -1);
        int base = seg.lineaInicio() != null ? seg.lineaInicio() : 1;
        List<SegmentoTexto> chunks = new ArrayList<>();
        int i = 0;
        while (i < lineas.length) {
            int inicio = i;
            int largo = 0;
            StringBuilder sb = new StringBuilder();
            while (i < lineas.length && (i == inicio || largo + lineas[i].length() + 1 <= props.maxChars())) {
                sb.append(lineas[i]).append('\n');
                largo += lineas[i].length() + 1;
                i++;
            }
            int finExclusivo = i;
            String texto = sb.toString().strip();
            if (!texto.isBlank()) {
                chunks.add(SegmentoTexto.de(seg.origen(), seg.seccion(), seg.lenguaje(),
                        base + inicio, base + finExclusivo - 1, texto).conArchivo(seg.archivoId()));
            }
            if (i >= lineas.length) {
                break;
            }
            i = retroceder(finExclusivo, inicio,
                    indice -> lineas[indice].length() + 1);
        }
        return chunks;
    }

    /** Prosa: empaqueta párrafos hasta el presupuesto, con solape por párrafos. */
    private List<SegmentoTexto> dividirProsa(SegmentoTexto seg) {
        List<String> unidades = new ArrayList<>();
        for (String parrafo : seg.contenido().split("\\n\\s*\\n")) {
            String p = parrafo.strip();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() <= props.maxChars()) {
                unidades.add(p);
            } else {
                unidades.addAll(trozos(p));
            }
        }
        List<SegmentoTexto> chunks = new ArrayList<>();
        int i = 0;
        while (i < unidades.size()) {
            int inicio = i;
            int largo = 0;
            StringBuilder sb = new StringBuilder();
            while (i < unidades.size()
                    && (i == inicio || largo + unidades.get(i).length() + 2 <= props.maxChars())) {
                sb.append(unidades.get(i)).append("\n\n");
                largo += unidades.get(i).length() + 2;
                i++;
            }
            int finExclusivo = i;
            chunks.add(SegmentoTexto.de(seg.origen(), seg.seccion(), null, null, null,
                    sb.toString().strip()).conArchivo(seg.archivoId()));
            if (i >= unidades.size()) {
                break;
            }
            i = retroceder(finExclusivo, inicio, indice -> unidades.get(indice).length() + 2);
        }
        return chunks;
    }

    /** Retrocede unidades desde {@code finExclusivo} hasta acumular ~overlapChars, con progreso. */
    private int retroceder(int finExclusivo, int inicio, java.util.function.IntUnaryOperator largoDe) {
        int retro = 0;
        int acumulado = 0;
        while (finExclusivo - 1 - retro > inicio
                && acumulado + largoDe.applyAsInt(finExclusivo - 1 - retro) <= props.overlapChars()) {
            acumulado += largoDe.applyAsInt(finExclusivo - 1 - retro);
            retro++;
        }
        int siguiente = finExclusivo - retro;
        return siguiente <= inicio ? inicio + 1 : siguiente;
    }

    private List<String> trozos(String texto) {
        List<String> partes = new ArrayList<>();
        for (int desde = 0; desde < texto.length(); desde += props.maxChars()) {
            partes.add(texto.substring(desde, Math.min(desde + props.maxChars(), texto.length())));
        }
        return partes;
    }
}
