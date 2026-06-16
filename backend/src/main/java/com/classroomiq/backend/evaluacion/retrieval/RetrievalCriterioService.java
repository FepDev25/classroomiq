package com.classroomiq.backend.evaluacion.retrieval;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.entrega.repository.FragmentoSimilar;
import com.classroomiq.backend.provider.embeddings.EmbeddingProvider;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;

/**
 * Retrieval semántico por criterio: el "RAG por criterio" del pipeline de evaluación (Fase 4).
 *
 * <p>Para un criterio de la rúbrica arma un texto-consulta (nombre + descripción + niveles de
 * desempeño), lo vectoriza con el mismo {@link EmbeddingProvider} que indexó la entrega, y recupera
 * los fragmentos de esa entrega más cercanos por similitud coseno. Esos fragmentos son la evidencia
 * que el motor (Hito 3) inserta en el prompt del LLM y que sustentan la justificación por criterio.
 */
@Service
public class RetrievalCriterioService {

    private final EmbeddingProvider embeddings;
    private final FragmentoEntregaRepository fragmentos;
    private final RetrievalProperties properties;

    public RetrievalCriterioService(EmbeddingProvider embeddings,
            FragmentoEntregaRepository fragmentos, RetrievalProperties properties) {
        this.embeddings = embeddings;
        this.fragmentos = fragmentos;
        this.properties = properties;
    }

    /**
     * Recupera los fragmentos más relevantes de la entrega para el criterio dado, ordenados de
     * mayor a menor relevancia (menor distancia coseno primero).
     *
     * <p>El {@code criterio} debe traer sus niveles cargados (la consulta los recorre); el llamador
     * lo invoca dentro de la sesión que cargó el agregado de la rúbrica. La búsqueda queda acotada
     * al tenant activo del {@link TenantContext} y a la {@code entregaId} indicada.
     */
    public List<FragmentoSimilar> recuperarParaCriterio(UUID entregaId, Criterio criterio) {
        float[] consulta = embeddings.embed(textoConsulta(criterio));
        UUID tenantId = TenantContext.getOrSinTenant();
        return fragmentos.buscarSimilaresEnEntrega(tenantId, entregaId, consulta, properties.topK());
    }

    /**
     * Construye el texto que representa al criterio para la búsqueda semántica: su nombre, su
     * descripción, y la descripción de cada nivel de desempeño. Es la "consulta" del retrieval.
     */
    String textoConsulta(Criterio criterio) {
        StringBuilder sb = new StringBuilder(criterio.getNombre());
        if (criterio.getDescripcion() != null && !criterio.getDescripcion().isBlank()) {
            sb.append('\n').append(criterio.getDescripcion());
        }
        for (NivelDesempeno nivel : criterio.getNiveles()) {
            sb.append('\n').append(nivel.getNombre());
            if (nivel.getDescripcion() != null && !nivel.getDescripcion().isBlank()) {
                sb.append(": ").append(nivel.getDescripcion());
            }
        }
        return sb.toString();
    }
}
