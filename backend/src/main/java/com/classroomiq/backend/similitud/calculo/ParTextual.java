package com.classroomiq.backend.similitud.calculo;

import java.util.List;
import java.util.UUID;

/**
 * Resultado del cálculo de similitud <em>textual</em> (n-gramas) para un par no ordenado de entregas
 * ({@code entregaAId < entregaBId} por convención). Lo produce el Hito 2; la orquestación (Hito 3)
 * lo fusiona con el {@link ParCalculado} semántico del mismo par.
 *
 * @param similitudTextual coeficiente de Jaccard de shingles en {@code [0, 1]}, o {@code null} si
 *                         alguna entrega no tiene prosa comparable (menos de {@code n} palabras).
 * @param fragmentos       pares de fragmentos con mayor solapamiento literal para la vista lado a lado.
 */
public record ParTextual(
        UUID entregaAId,
        UUID entregaBId,
        Double similitudTextual,
        List<MatchFragmento> fragmentos) {
}
