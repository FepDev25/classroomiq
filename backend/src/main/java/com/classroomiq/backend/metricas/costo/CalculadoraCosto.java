package com.classroomiq.backend.metricas.costo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.classroomiq.backend.metricas.costo.CostoProperties.Tarifa;

/**
 * Convierte tokens consumidos en costo estimado según las tarifas configuradas (Fase 6).
 *
 * <p>{@code costo = inputTokens·tarifaIn/1e6 + outputTokens·tarifaOut/1e6}. Si el modelo no tiene
 * tarifa configurada, devuelve cero (el uso en tokens sigue siendo visible; solo no se le imputa
 * costo). El emparejamiento es exacto y, en su defecto, por prefijo — para tolerar ids de modelo
 * con sufijo de fecha que el proveedor pueda devolver (ej. {@code claude-sonnet-4-6-2025...}).
 */
@Component
public class CalculadoraCosto {

    /** Escala de redondeo del costo: 4 decimales captan estimaciones de centavos por entrega. */
    private static final int ESCALA = 4;
    private static final BigDecimal POR_MILLON = BigDecimal.valueOf(1_000_000L);

    private final CostoProperties props;

    public CalculadoraCosto(CostoProperties props) {
        this.props = props;
    }

    /** Costo estimado del consumo dado, redondeado a {@value #ESCALA} decimales (nunca nulo). */
    public BigDecimal costo(String modelo, long inputTokens, long outputTokens) {
        Tarifa tarifa = tarifaDe(modelo);
        if (tarifa == null) {
            return BigDecimal.ZERO.setScale(ESCALA);
        }
        BigDecimal costoIn = BigDecimal.valueOf(inputTokens).multiply(tarifa.inputPorMillon());
        BigDecimal costoOut = BigDecimal.valueOf(outputTokens).multiply(tarifa.outputPorMillon());
        return costoIn.add(costoOut).divide(POR_MILLON, ESCALA, RoundingMode.HALF_UP);
    }

    public BigDecimal umbralMensual() {
        return props.umbralMensual();
    }

    public String moneda() {
        return props.moneda();
    }

    private Tarifa tarifaDe(String modelo) {
        if (modelo == null) {
            return null;
        }
        Map<String, Tarifa> tarifas = props.tarifas();
        Tarifa exacta = tarifas.get(modelo);
        if (exacta != null) {
            return exacta;
        }
        return tarifas.entrySet().stream()
                .filter(e -> modelo.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
