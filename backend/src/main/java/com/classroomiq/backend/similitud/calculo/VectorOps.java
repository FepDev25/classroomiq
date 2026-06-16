package com.classroomiq.backend.similitud.calculo;

import java.util.List;

/**
 * Operaciones vectoriales para la similitud semántica (Fase 5).
 *
 * <p>Los embeddings de {@code fragmento_entrega} ya están normalizados L2, por lo que la similitud
 * coseno entre dos de ellos es su producto punto. El "vector de la entrega" es el centroide
 * (promedio) de los embeddings de sus fragmentos, re-normalizado L2 para que el coseno entre
 * centroides siga siendo el producto punto.
 *
 * <p>Clase de utilidad pura (sin estado, sin dependencias) para poder testearla sin contexto Spring.
 */
final class VectorOps {

    private VectorOps() {
    }

    /**
     * Centroide normalizado L2 de una lista de vectores. Devuelve {@code null} si la lista está
     * vacía o el promedio es el vector cero (norma 0), casos en que la entrega no tiene contenido
     * vectorizable comparable.
     */
    static float[] centroide(List<float[]> vectores) {
        if (vectores == null || vectores.isEmpty()) {
            return null;
        }
        int dim = vectores.get(0).length;
        double[] suma = new double[dim];
        for (float[] v : vectores) {
            for (int i = 0; i < dim; i++) {
                suma[i] += v[i];
            }
        }
        float[] centroide = new float[dim];
        for (int i = 0; i < dim; i++) {
            centroide[i] = (float) (suma[i] / vectores.size());
        }
        return normalizarL2(centroide);
    }

    /**
     * Normaliza un vector a norma L2 = 1. Devuelve {@code null} si la norma es 0 (vector nulo).
     */
    static float[] normalizarL2(float[] v) {
        double normaCuadrado = 0.0;
        for (float x : v) {
            normaCuadrado += (double) x * x;
        }
        if (normaCuadrado == 0.0) {
            return null;
        }
        double norma = Math.sqrt(normaCuadrado);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = (float) (v[i] / norma);
        }
        return out;
    }

    /**
     * Similitud coseno de dos vectores normalizados L2 (= producto punto), acotada a {@code [0, 1]}:
     * una similitud negativa (vectores en sentidos opuestos) no aporta como evidencia y se reporta
     * como 0. Devuelve 0 si alguno es {@code null} o de dimensión distinta.
     */
    static double coseno(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double punto = 0.0;
        for (int i = 0; i < a.length; i++) {
            punto += (double) a[i] * b[i];
        }
        if (punto < 0.0) {
            return 0.0;
        }
        return Math.min(punto, 1.0);
    }
}
