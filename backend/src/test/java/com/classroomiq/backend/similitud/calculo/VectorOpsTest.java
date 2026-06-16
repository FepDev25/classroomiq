package com.classroomiq.backend.similitud.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests de la aritmética vectorial de la similitud semántica: centroide, normalización, coseno. */
class VectorOpsTest {

    @Test
    void cosenoDeVectoresIdenticosNormalizadosEsUno() {
        float[] v = VectorOps.normalizarL2(new float[] {3f, 4f});
        assertThat(VectorOps.coseno(v, v)).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void cosenoDeVectoresOrtogonalesEsCero() {
        float[] a = VectorOps.normalizarL2(new float[] {1f, 0f});
        float[] b = VectorOps.normalizarL2(new float[] {0f, 1f});
        assertThat(VectorOps.coseno(a, b)).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void cosenoNegativoSeAcotaACero() {
        float[] a = VectorOps.normalizarL2(new float[] {1f, 0f});
        float[] b = VectorOps.normalizarL2(new float[] {-1f, 0f});
        assertThat(VectorOps.coseno(a, b)).isZero();
    }

    @Test
    void cosenoConVectorNuloOdimensionDistintaEsCero() {
        float[] a = new float[] {1f, 0f};
        assertThat(VectorOps.coseno(a, null)).isZero();
        assertThat(VectorOps.coseno(a, new float[] {1f, 0f, 0f})).isZero();
    }

    @Test
    void centroideEsElPromedioNormalizado() {
        // Promedio de (1,0) y (0,1) = (0.5,0.5); normalizado ≈ (0.7071, 0.7071).
        float[] c = VectorOps.centroide(List.of(new float[] {1f, 0f}, new float[] {0f, 1f}));
        assertThat(c).isNotNull();
        double inv = 1.0 / Math.sqrt(2.0);
        assertThat((double) c[0]).isCloseTo(inv, within(1e-6));
        assertThat((double) c[1]).isCloseTo(inv, within(1e-6));
        // Norma resultante = 1.
        assertThat(VectorOps.coseno(c, c)).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void centroideDeListaVaciaEsNull() {
        assertThat(VectorOps.centroide(List.of())).isNull();
    }

    @Test
    void normalizarVectorCeroEsNull() {
        assertThat(VectorOps.normalizarL2(new float[] {0f, 0f})).isNull();
    }
}
