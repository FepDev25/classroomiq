package com.classroomiq.backend.reportes.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests del resumen estadístico de notas: promedio, mediana (par/impar), mínimo y máximo. */
class EstadisticasTest {

    @Test
    void resumenConCantidadImparUsaElElementoCentral() {
        Estadisticas.Resumen r = Estadisticas.resumir(List.of(10.0, 6.0, 8.0));
        assertThat(r.promedio()).isCloseTo(8.0, within(1e-9));
        assertThat(r.mediana()).isCloseTo(8.0, within(1e-9));
        assertThat(r.minima()).isEqualTo(6.0);
        assertThat(r.maxima()).isEqualTo(10.0);
    }

    @Test
    void medianaConCantidadParEsPromedioDeLosDosCentrales() {
        Estadisticas.Resumen r = Estadisticas.resumir(List.of(10.0, 6.0, 8.0, 4.0));
        assertThat(r.mediana()).isCloseTo(7.0, within(1e-9)); // (6+8)/2
        assertThat(r.promedio()).isCloseTo(7.0, within(1e-9));
    }

    @Test
    void listaVaciaLanza() {
        assertThatThrownBy(() -> Estadisticas.resumir(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
