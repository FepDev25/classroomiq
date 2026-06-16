package com.classroomiq.backend.similitud.calculo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilidades de similitud textual por n-gramas de palabras (Fase 5, Hito 2).
 *
 * <p>Complementa la similitud semántica: detecta fragmentos copiados <em>literalmente</em>. El texto
 * se normaliza (minúsculas, sin acentos, sin puntuación, espacios colapsados), se tokeniza en
 * palabras y se construyen "shingles" (secuencias de {@code n} palabras consecutivas). La similitud
 * entre dos conjuntos de shingles es el coeficiente de Jaccard {@code |A∩B| / |A∪B|}, simétrico y en
 * {@code [0, 1]} — apto para la matriz de calor y el ordenamiento de pares.
 *
 * <p>Clase de utilidad pura (sin estado) para testearla sin contexto Spring.
 */
final class NgramTextual {

    private NgramTextual() {
    }

    /** Tokeniza el texto en palabras normalizadas (minúsculas, sin acentos ni puntuación). */
    static List<String> tokenizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String normal = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        List<String> tokens = new ArrayList<>();
        for (String t : normal.split("[^\\p{L}\\p{N}]+")) {
            if (!t.isEmpty()) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    /**
     * Conjunto de shingles de {@code n} palabras del texto. Vacío si el texto tiene menos de
     * {@code n} tokens (no hay secuencia completa que comparar).
     */
    static Set<String> shingles(String texto, int n) {
        List<String> tokens = tokenizar(texto);
        Set<String> shingles = new HashSet<>();
        if (tokens.size() < n) {
            return shingles;
        }
        for (int i = 0; i + n <= tokens.size(); i++) {
            shingles.add(String.join(" ", tokens.subList(i, i + n)));
        }
        return shingles;
    }

    /**
     * Coeficiente de Jaccard entre dos conjuntos de shingles. Devuelve 0 si ambos están vacíos
     * (la intersección y la unión son vacías) — el caso "sin prosa comparable" lo decide el llamador.
     */
    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }
        int interseccion = 0;
        Set<String> menor = a.size() <= b.size() ? a : b;
        Set<String> mayor = a.size() <= b.size() ? b : a;
        for (String s : menor) {
            if (mayor.contains(s)) {
                interseccion++;
            }
        }
        int union = a.size() + b.size() - interseccion;
        return union == 0 ? 0.0 : (double) interseccion / union;
    }
}
