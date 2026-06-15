package com.classroomiq.backend.entrega.procesamiento;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.FragmentoEntrega;
import com.classroomiq.backend.entrega.extraccion.SegmentoTexto;
import com.classroomiq.backend.entrega.extraccion.ServicioExtraccion;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.provider.embeddings.EmbeddingProvider;

/**
 * Pipeline de indexación de una entrega: extrae el texto de sus archivos, lo divide en chunks,
 * genera los embeddings y los persiste como {@link FragmentoEntrega} en pgvector.
 *
 * <p>Es idempotente: borra los fragmentos previos de la entrega antes de reindexar (reproceso).
 * Requiere que el {@code TenantContext} esté fijado por el llamador (lo hará el worker del Hito 5);
 * de ahí, el {@code @TenantId} de Hibernate estampa el tenant en cada fragmento.
 *
 * <p>El embedding (llamada externa a Ollama) se hace fuera de transacción; el borrado y el guardado
 * usan las transacciones propias del repositorio para no retener una conexión durante el HTTP.
 */
@Service
public class ProcesadorEntrega {

    private final ServicioExtraccion extraccion;
    private final Chunker chunker;
    private final EmbeddingProvider embeddings;
    private final FragmentoEntregaRepository fragmentos;

    public ProcesadorEntrega(ServicioExtraccion extraccion, Chunker chunker,
            EmbeddingProvider embeddings, FragmentoEntregaRepository fragmentos) {
        this.extraccion = extraccion;
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.fragmentos = fragmentos;
    }

    /**
     * Indexa la entrega a partir de sus archivos y devuelve el número de fragmentos generados.
     *
     * @return cantidad de fragmentos persistidos (0 si la entrega no produjo texto)
     */
    public int indexar(Entrega entrega, List<ArchivoEntrega> archivos) {
        List<SegmentoTexto> chunks = chunker.dividir(extraccion.extraer(archivos));

        fragmentos.deleteByEntregaId(entrega.getId());
        if (chunks.isEmpty()) {
            return 0;
        }

        List<float[]> vectores = embeddings.embed(chunks.stream().map(SegmentoTexto::contenido).toList());
        List<FragmentoEntrega> entidades = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            entidades.add(aFragmento(entrega, chunks.get(i), i, vectores.get(i)));
        }
        fragmentos.saveAll(entidades);
        return entidades.size();
    }

    private FragmentoEntrega aFragmento(Entrega entrega, SegmentoTexto chunk, int orden, float[] embedding) {
        FragmentoEntrega frag = new FragmentoEntrega();
        frag.setDocenteId(entrega.getDocenteId());
        frag.setMateriaId(entrega.getMateriaId());
        frag.setLoteId(entrega.getLoteId());
        frag.setEntregaId(entrega.getId());
        frag.setArchivoId(chunk.archivoId());
        frag.setOrden(orden);
        frag.setContenido(chunk.contenido());
        frag.setSeccion(chunk.seccion());
        frag.setLineaInicio(chunk.lineaInicio());
        frag.setLineaFin(chunk.lineaFin());
        frag.setEmbedding(embedding);
        return frag;
    }
}
