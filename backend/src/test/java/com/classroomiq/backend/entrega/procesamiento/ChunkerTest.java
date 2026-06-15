package com.classroomiq.backend.entrega.procesamiento;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.classroomiq.backend.entrega.extraccion.SegmentoTexto;

/** Tests del chunking: corte de código por líneas con solape, prosa por párrafos, y paso directo. */
class ChunkerTest {

    private final Chunker chunker = new Chunker(new ChunkingProperties(100, 20));

    @Test
    void segmentoQueCabePasaTalCual() {
        SegmentoTexto seg = SegmentoTexto.de("f.txt", "Sec", null, null, null, "texto corto");
        assertThat(chunker.dividir(List.of(seg))).containsExactly(seg);
    }

    @Test
    void codigoLargoSeDivideConRangosDeLineaYSolape() {
        UUID archivoId = UUID.randomUUID();
        String codigo = IntStream.rangeClosed(1, 20)
                .mapToObj(n -> "linea " + n + " de codigo")
                .collect(Collectors.joining("\n"));
        SegmentoTexto seg = new SegmentoTexto(archivoId, "src/main.py", null, "Python", 1, 20, codigo);

        List<SegmentoTexto> chunks = chunker.dividir(List.of(seg));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> {
            assertThat(c.lenguaje()).isEqualTo("Python");
            assertThat(c.archivoId()).isEqualTo(archivoId);
            assertThat(c.contenido()).isNotBlank();
        });
        // Cubre desde la línea 1 hasta la 20.
        assertThat(chunks.get(0).lineaInicio()).isEqualTo(1);
        assertThat(chunks.get(chunks.size() - 1).lineaFin()).isEqualTo(20);
        // Solape: cada chunk empieza en o antes del fin del anterior.
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).lineaInicio()).isLessThanOrEqualTo(chunks.get(i - 1).lineaFin());
        }
    }

    @Test
    void prosaLargaSeDividePorParrafos() {
        String prosa = IntStream.rangeClosed(1, 8)
                .mapToObj(n -> "Parrafo numero " + n + " con contenido de relleno.")
                .collect(Collectors.joining("\n\n"));
        SegmentoTexto seg = SegmentoTexto.de("informe.pdf", "Introducción", null, null, null, prosa);

        List<SegmentoTexto> chunks = chunker.dividir(List.of(seg));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> {
            assertThat(c.seccion()).isEqualTo("Introducción");
            assertThat(c.lenguaje()).isNull();
            assertThat(c.lineaInicio()).isNull();
            assertThat(c.contenido().length()).isLessThanOrEqualTo(100 + 60);
        });
    }
}
