package com.classroomiq.backend.evaluacion.motor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.classroomiq.backend.rubrica.domain.NivelDesempeno;

/**
 * Rango de puntos absolutos [min, max] que admite un nivel de desempeño, resuelto desde las tres
 * formas de {@code TipoPuntaje} (ver SCHEMA.md): RANGO usa sus límites; FIJO es un punto único;
 * BANDA_PCT se traduce a puntos sobre el {@code puntajeMaximo} del criterio.
 *
 * <p>Su razón de ser en el motor: garantizar la regla inamovible "no asignar un puntaje fuera del
 * rango del nivel sugerido". El motor {@link #ajustar(BigDecimal) acota} el puntaje del LLM al rango.
 */
public record RangoPuntaje(BigDecimal min, BigDecimal max) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public static RangoPuntaje de(NivelDesempeno nivel, BigDecimal puntajeMaximoCriterio) {
        return switch (nivel.getTipoPuntaje()) {
            case RANGO -> new RangoPuntaje(nivel.getPuntajeMin(), nivel.getPuntajeMax());
            case FIJO -> new RangoPuntaje(nivel.getPuntajeValor(), nivel.getPuntajeValor());
            case BANDA_PCT -> new RangoPuntaje(
                    pctAPuntos(nivel.getPctMin(), puntajeMaximoCriterio),
                    pctAPuntos(nivel.getPctMax(), puntajeMaximoCriterio));
        };
    }

    /** Acota un puntaje al rango: por debajo del mínimo devuelve el mínimo, por encima el máximo. */
    public BigDecimal ajustar(BigDecimal puntaje) {
        if (puntaje == null) {
            return min;
        }
        if (puntaje.compareTo(min) < 0) {
            return min;
        }
        if (puntaje.compareTo(max) > 0) {
            return max;
        }
        return puntaje;
    }

    public boolean contiene(BigDecimal puntaje) {
        return puntaje != null && puntaje.compareTo(min) >= 0 && puntaje.compareTo(max) <= 0;
    }

    private static BigDecimal pctAPuntos(BigDecimal pct, BigDecimal maximo) {
        return maximo.multiply(pct).divide(CIEN, 2, RoundingMode.HALF_UP);
    }
}
