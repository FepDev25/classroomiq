package com.classroomiq.backend.similitud.calculo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.classroomiq.backend.entrega.domain.FragmentoEntrega;
import com.classroomiq.backend.entrega.repository.FragmentoEntregaRepository;
import com.classroomiq.backend.similitud.SimilitudProperties;
import com.classroomiq.backend.similitud.domain.TipoSimilitud;

/**
 * Cálculo de similitud textual (n-gramas de palabras) entre las entregas de un lote (Fase 5,
 * Hito 2). Complementa la semántica: detecta fragmentos copiados literalmente.
 *
 * <p>Para cada entrega construye el conjunto de shingles de tamaño {@code ngramaTextual} (unión de
 * los shingles de sus fragmentos, para no crear n-gramas espurios al cruzar fronteras de chunk). La
 * similitud del par es el coeficiente de Jaccard de esos conjuntos; los pares de fragmentos con
 * mayor solapamiento literal alimentan la vista lado a lado.
 *
 * <p>Es aritmética sobre el texto ya extraído (Fase 3), sin llamadas externas. Acotado al lote (el
 * llamador pasa solo las entregas LISTO) y al tenant activo (los repos aplican {@code @TenantId}).
 */
@Service
public class SimilitudTextualService {

    private final FragmentoEntregaRepository fragmentos;
    private final SimilitudProperties properties;

    public SimilitudTextualService(FragmentoEntregaRepository fragmentos,
            SimilitudProperties properties) {
        this.fragmentos = fragmentos;
        this.properties = properties;
    }

    /**
     * Calcula la similitud textual de cada par no ordenado de las entregas dadas (convención
     * {@code entregaAId < entregaBId}). {@code similitudTextual} es {@code null} cuando alguna
     * entrega no tiene shingles (menos de {@code n} palabras de prosa comparable).
     */
    public List<ParTextual> calcular(List<UUID> entregaIds) {
        int n = properties.ngramaTextual();
        List<UUID> ordenadas = entregaIds.stream().sorted().toList();

        Map<UUID, List<FragmentoEntrega>> porEntrega = new LinkedHashMap<>();
        Map<UUID, Set<String>> shinglesEntrega = new LinkedHashMap<>();
        for (UUID id : ordenadas) {
            List<FragmentoEntrega> frags = fragmentos.findAllByEntregaIdOrderByOrdenAsc(id);
            porEntrega.put(id, frags);
            Set<String> union = new java.util.HashSet<>();
            for (FragmentoEntrega f : frags) {
                union.addAll(NgramTextual.shingles(f.getContenido(), n));
            }
            shinglesEntrega.put(id, union);
        }

        List<ParTextual> pares = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            for (int j = i + 1; j < ordenadas.size(); j++) {
                UUID a = ordenadas.get(i);
                UUID b = ordenadas.get(j);
                Set<String> sa = shinglesEntrega.get(a);
                Set<String> sb = shinglesEntrega.get(b);
                Double similitud = (sa.isEmpty() || sb.isEmpty()) ? null : NgramTextual.jaccard(sa, sb);
                List<MatchFragmento> top = topFragmentos(porEntrega.get(a), porEntrega.get(b), n);
                pares.add(new ParTextual(a, b, similitud, top));
            }
        }
        return pares;
    }

    /**
     * Encuentra los {@code topFragmentos} pares de fragmentos con mayor solapamiento literal (Jaccard
     * de shingles), con un min-heap acotado. Devuelve los pares ordenados de mayor a menor.
     */
    private List<MatchFragmento> topFragmentos(List<FragmentoEntrega> fa, List<FragmentoEntrega> fb, int n) {
        int limite = properties.topFragmentos();
        Map<UUID, Set<String>> cacheA = new LinkedHashMap<>();
        Map<UUID, Set<String>> cacheB = new LinkedHashMap<>();
        for (FragmentoEntrega f : fa) {
            cacheA.put(f.getId(), NgramTextual.shingles(f.getContenido(), n));
        }
        for (FragmentoEntrega f : fb) {
            cacheB.put(f.getId(), NgramTextual.shingles(f.getContenido(), n));
        }

        PriorityQueue<MatchFragmento> heap =
                new PriorityQueue<>(Comparator.comparingDouble(MatchFragmento::similitud));
        for (FragmentoEntrega a : fa) {
            Set<String> sa = cacheA.get(a.getId());
            if (sa.isEmpty()) {
                continue;
            }
            for (FragmentoEntrega b : fb) {
                Set<String> sb = cacheB.get(b.getId());
                if (sb.isEmpty()) {
                    continue;
                }
                double sim = NgramTextual.jaccard(sa, sb);
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
                TipoSimilitud.TEXTUAL, a.getId(), a.getContenido(), b.getId(), b.getContenido(), sim);
    }
}
