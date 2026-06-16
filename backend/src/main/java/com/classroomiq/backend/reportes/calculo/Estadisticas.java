package com.classroomiq.backend.reportes.calculo;

import java.util.ArrayList;
import java.util.List;

/**
 * Estadísticas descriptivas de un conjunto de notas. Utilidad pura, sin estado,
 * para testearla sin contexto Spring.
 */
public final class Estadisticas {

    private Estadisticas() {
    }

    /** Resumen de un conjunto de notas: promedio, mediana, mínimo y máximo. */
    public record Resumen(double promedio, double mediana, double minima, double maxima) {
    }

    /**
     * Calcula el resumen de las notas dadas. Lanza si la lista está vacía (el llamador garantiza que
     * hay al menos una evaluación aprobada).
     */
    public static Resumen resumir(List<Double> notas) {
        if (notas.isEmpty()) {
            throw new IllegalArgumentException("No hay notas para resumir");
        }
        List<Double> orden = new ArrayList<>(notas);
        orden.sort(Double::compareTo);
        double suma = orden.stream().mapToDouble(Double::doubleValue).sum();
        return new Resumen(
                suma / orden.size(),
                mediana(orden),
                orden.get(0),
                orden.get(orden.size() - 1));
    }

    /** Mediana de una lista YA ordenada ascendentemente. */
    private static double mediana(List<Double> ordenadas) {
        int n = ordenadas.size();
        int medio = n / 2;
        if (n % 2 == 1) {
            return ordenadas.get(medio);
        }
        return (ordenadas.get(medio - 1) + ordenadas.get(medio)) / 2.0;
    }
}
