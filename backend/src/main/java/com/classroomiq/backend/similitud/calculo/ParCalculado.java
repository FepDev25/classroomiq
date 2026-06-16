package com.classroomiq.backend.similitud.calculo;

import java.util.List;
import java.util.UUID;

/**
 * Resultado del cálculo de similitud <em>semántica</em> para un par no ordenado de entregas
 * ({@code entregaAId < entregaBId} por convención). Lo produce el Hito 1; el textual va en
 * {@link ParTextual}. La orquestación (Hito 3) fusiona ambos y los persiste como
 * {@code par_similitud} + fragmentos.
 *
 * @param similitudSemantica coseno de los centroides de embeddings, en {@code [0, 1]}.
 * @param fragmentos         pares de fragmentos de mayor similitud semántica para la vista lado a lado.
 */
public record ParCalculado(
        UUID entregaAId,
        UUID entregaBId,
        double similitudSemantica,
        List<MatchFragmento> fragmentos) {
}
