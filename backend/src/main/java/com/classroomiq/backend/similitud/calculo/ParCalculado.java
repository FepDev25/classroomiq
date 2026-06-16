package com.classroomiq.backend.similitud.calculo;

import java.util.List;
import java.util.UUID;

/**
 * Resultado del cálculo de similitud para un par no ordenado de entregas ({@code entregaAId <
 * entregaBId} por convención). Lo produce el cálculo semántico (Hito 1) y lo enriquece el textual
 * (Hito 2); la orquestación (Hito 3) lo persiste como {@code par_similitud} + fragmentos.
 *
 * @param similitudSemantica coseno de los centroides de embeddings, en {@code [0, 1]}.
 * @param similitudTextual   solapamiento de n-gramas en {@code [0, 1]}, o {@code null} si alguna
 *                           entrega no tiene prosa comparable (lo completa el Hito 2).
 * @param fragmentos         pares de fragmentos de mayor similitud para la vista lado a lado.
 */
public record ParCalculado(
        UUID entregaAId,
        UUID entregaBId,
        double similitudSemantica,
        Double similitudTextual,
        List<MatchFragmento> fragmentos) {
}
