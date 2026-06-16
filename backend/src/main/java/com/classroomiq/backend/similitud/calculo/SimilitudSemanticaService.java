package com.classroomiq.backend.similitud.calculo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.classroomiq.backend.entrega.domain.FragmentoEntrega;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.similitud.SimilitudProperties;
import com.classroomiq.backend.similitud.domain.TipoSimilitud;

/**
 * Cálculo de similitud semántica entre las entregas de un lote (Fase 5, Hito 1).
 *
 * <p>Los fragmentos de cada entrega ya tienen su embedding normalizado L2 (Fase 3), por lo que el
 * cálculo es aritmética vectorial pura — sin llamadas al proveedor de embeddings. Para cada entrega
 * se obtiene su centroide; la similitud del par es el coseno de centroides. Además, para la
 * visualización lado a lado, se identifican los pares de fragmentos individuales de mayor similitud.
 *
 * <p>La búsqueda queda acotada al lote (el llamador pasa solo las entregas LISTO del lote) y al
 * tenant activo (los repos aplican {@code @TenantId} en las consultas derivadas).
 */
@Service
public class SimilitudSemanticaService {

    private final FragmentoEntregaRepository fragmentos;
    private final SimilitudProperties properties;

    public SimilitudSemanticaService(FragmentoEntregaRepository fragmentos,
            SimilitudProperties properties) {
        this.fragmentos = fragmentos;
        this.properties = properties;
    }

    /**
     * Calcula la similitud semántica de cada par no ordenado de las entregas dadas. Las entregas se
     * ordenan por id para fijar la convención {@code entregaAId < entregaBId} y no duplicar pares.
     * Devuelve lista vacía si hay menos de dos entregas comparables.
     */
    public List<ParCalculado> calcular(List<UUID> entregaIds) {
        List<UUID> ordenadas = entregaIds.stream().sorted().toList();

        Map<UUID, List<FragmentoEntrega>> porEntrega = new LinkedHashMap<>();
        Map<UUID, float[]> centroides = new LinkedHashMap<>();
        for (UUID id : ordenadas) {
            List<FragmentoEntrega> frags = fragmentos.findAllByEntregaIdOrderByOrdenAsc(id);
            porEntrega.put(id, frags);
            centroides.put(id, VectorOps.centroide(frags.stream().map(FragmentoEntrega::getEmbedding).toList()));
        }

        List<ParCalculado> pares = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            for (int j = i + 1; j < ordenadas.size(); j++) {
                UUID a = ordenadas.get(i);
                UUID b = ordenadas.get(j);
                double similitud = VectorOps.coseno(centroides.get(a), centroides.get(b));
                List<MatchFragmento> top = topFragmentos(porEntrega.get(a), porEntrega.get(b));
                pares.add(new ParCalculado(a, b, similitud, top));
            }
        }
        return pares;
    }

    /**
     * Encuentra los {@code topFragmentos} pares de fragmentos (uno de A, uno de B) de mayor similitud
     * coseno, mediante un min-heap acotado para no materializar todas las combinaciones. Devuelve los
     * pares ordenados de mayor a menor similitud.
     */
    private List<MatchFragmento> topFragmentos(List<FragmentoEntrega> fa, List<FragmentoEntrega> fb) {
        int limite = properties.topFragmentos();
        PriorityQueue<MatchFragmento> heap =
                new PriorityQueue<>(Comparator.comparingDouble(MatchFragmento::similitud));
        for (FragmentoEntrega a : fa) {
            for (FragmentoEntrega b : fb) {
                double sim = VectorOps.coseno(a.getEmbedding(), b.getEmbedding());
                if (sim <= 0.0) {
                    continue;
                }
                if (heap.size() < limite) {
                    heap.add(match(a, b, sim));
                } else if (sim > heap.peek().similitud()) {
                    heap.poll();
                    heap.add(match(a, b, sim));
                }
            }
        }
        List<MatchFragmento> top = new ArrayList<>(heap);
        top.sort(Comparator.comparingDouble(MatchFragmento::similitud).reversed());
        return top;
    }

    private MatchFragmento match(FragmentoEntrega a, FragmentoEntrega b, double sim) {
        return new MatchFragmento(
                TipoSimilitud.SEMANTICA, a.getId(), a.getContenido(), b.getId(), b.getContenido(), sim);
    }
}
