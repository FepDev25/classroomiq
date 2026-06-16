package com.classroomiq.backend.similitud.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** Tests de la similitud textual por n-gramas: tokenización, shingles y Jaccard. */
class NgramTextualTest {

    @Test
    void tokenizaEnMinusculasSinAcentosNiPuntuacion() {
        assertThat(NgramTextual.tokenizar("¡Árbol, rápido!  el-fin"))
                .containsExactly("arbol", "rapido", "el", "fin");
    }

    @Test
    void shinglesDeTamanoN() {
        Set<String> s = NgramTextual.shingles("uno dos tres cuatro", 2);
        assertThat(s).containsExactlyInAnyOrder("uno dos", "dos tres", "tres cuatro");
    }

    @Test
    void textoMasCortoQueNNoProduceShingles() {
        assertThat(NgramTextual.shingles("uno dos", 3)).isEmpty();
    }

    @Test
    void jaccardDeConjuntosIdenticosEsUno() {
        Set<String> s = NgramTextual.shingles("la rapida zorra parda salta", 3);
        assertThat(NgramTextual.jaccard(s, s)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void jaccardSinSolapamientoEsCero() {
        Set<String> a = NgramTextual.shingles("uno dos tres", 2);
        Set<String> b = NgramTextual.shingles("cuatro cinco seis", 2);
        assertThat(NgramTextual.jaccard(a, b)).isZero();
    }

    @Test
    void jaccardParcialEsInterseccionSobreUnion() {
        // a = {uno dos, dos tres}; b = {dos tres, tres cuatro}; ∩=1, ∪=3 -> 1/3.
        Set<String> a = NgramTextual.shingles("uno dos tres", 2);
        Set<String> b = NgramTextual.shingles("dos tres cuatro", 2);
        assertThat(NgramTextual.jaccard(a, b)).isCloseTo(1.0 / 3.0, within(1e-9));
    }

    @Test
    void jaccardDeDosVaciosEsCero() {
        assertThat(NgramTextual.jaccard(Set.of(), Set.of())).isZero();
    }
}
