package com.classroomiq.backend.rubrica;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.rubrica.domain.ModoTotal;
import com.classroomiq.backend.rubrica.dto.CriterioRequest;
import com.classroomiq.backend.rubrica.dto.NivelRequest;
import com.classroomiq.backend.rubrica.dto.RubricaRequest;

/**
 * Reglas de coherencia de una rúbrica (más allá de la validación estructural de los DTOs):
 * cierre del total según el modo, mínimo de niveles, y puntajes de cada nivel dentro del rango
 * del criterio. Implementa el modelo de SCHEMA.md.
 */
@Component
public class RubricaValidator {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int MIN_NIVELES = 2;

    public void validar(RubricaRequest rubrica) {
        BigDecimal total = rubrica.puntajeTotal();
        BigDecimal sumaCriterios = BigDecimal.ZERO;

        for (CriterioRequest criterio : rubrica.criterios()) {
            if (criterio.niveles().size() < MIN_NIVELES) {
                throw new ReglaNegocioException(
                        "El criterio '" + criterio.nombre() + "' debe tener al menos " + MIN_NIVELES + " niveles");
            }

            BigDecimal maximo = criterio.puntajeMaximo();
            sumaCriterios = sumaCriterios.add(maximo);

            if (rubrica.modoTotal() == ModoTotal.PROMEDIO && maximo.compareTo(total) != 0) {
                throw new ReglaNegocioException("En modo promedio, el puntaje máximo de cada criterio debe igualar"
                        + " el total (" + total + "). El criterio '" + criterio.nombre() + "' tiene " + maximo);
            }

            for (NivelRequest nivel : criterio.niveles()) {
                validarNivel(criterio.nombre(), nivel, maximo);
            }
        }

        if (rubrica.modoTotal() == ModoTotal.SUMA && sumaCriterios.compareTo(total) != 0) {
            throw new ReglaNegocioException("En modo suma, la suma de los puntajes máximos de los criterios ("
                    + sumaCriterios + ") debe igualar el total de la rúbrica (" + total + ")");
        }
    }

    private void validarNivel(String criterio, NivelRequest nivel, BigDecimal maximo) {
        switch (nivel.tipoPuntaje()) {
            case RANGO -> {
                exigir(nivel.puntajeMin() != null && nivel.puntajeMax() != null, criterio, nivel,
                        "los niveles de tipo RANGO requieren puntajeMin y puntajeMax");
                exigir(nivel.puntajeMin().compareTo(BigDecimal.ZERO) >= 0, criterio, nivel,
                        "el puntaje mínimo no puede ser negativo");
                exigir(nivel.puntajeMax().compareTo(nivel.puntajeMin()) >= 0, criterio, nivel,
                        "el puntaje máximo no puede ser menor que el mínimo");
                exigir(nivel.puntajeMax().compareTo(maximo) <= 0, criterio, nivel,
                        "el puntaje máximo no puede superar el del criterio (" + maximo + ")");
            }
            case FIJO -> {
                exigir(nivel.puntajeValor() != null, criterio, nivel,
                        "los niveles de tipo FIJO requieren puntajeValor");
                exigir(nivel.puntajeValor().compareTo(BigDecimal.ZERO) >= 0, criterio, nivel,
                        "el puntaje no puede ser negativo");
                exigir(nivel.puntajeValor().compareTo(maximo) <= 0, criterio, nivel,
                        "el puntaje no puede superar el del criterio (" + maximo + ")");
            }
            case BANDA_PCT -> {
                exigir(nivel.pctMin() != null && nivel.pctMax() != null, criterio, nivel,
                        "los niveles de tipo BANDA_PCT requieren pctMin y pctMax");
                exigir(nivel.pctMin().compareTo(BigDecimal.ZERO) >= 0, criterio, nivel,
                        "el porcentaje mínimo no puede ser negativo");
                exigir(nivel.pctMax().compareTo(nivel.pctMin()) >= 0, criterio, nivel,
                        "el porcentaje máximo no puede ser menor que el mínimo");
                exigir(nivel.pctMax().compareTo(CIEN) <= 0, criterio, nivel,
                        "el porcentaje máximo no puede superar 100");
            }
        }
    }

    private void exigir(boolean condicion, String criterio, NivelRequest nivel, String detalle) {
        if (!condicion) {
            throw new ReglaNegocioException(
                    "Criterio '" + criterio + "', nivel '" + nivel.nombre() + "': " + detalle);
        }
    }
}
