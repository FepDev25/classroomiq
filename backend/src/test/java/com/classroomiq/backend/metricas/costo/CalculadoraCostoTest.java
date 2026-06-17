package com.classroomiq.backend.metricas.costo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.classroomiq.backend.metricas.costo.CostoProperties.Tarifa;

/**
 * Unit del cálculo de costo (Fase 6, sin Spring): tarifa por millón de tokens, redondeo, modelo sin
 * tarifa (costo cero) y emparejamiento por prefijo para ids con sufijo de fecha.
 */
class CalculadoraCostoTest {

    private final CalculadoraCosto calc = new CalculadoraCosto(new CostoProperties(
            "USD", new BigDecimal("50.00"), Map.of(
            "claude-sonnet-4-6", new Tarifa(new BigDecimal("3.00"), new BigDecimal("15.00")),
            "claude-haiku-4-5", new Tarifa(new BigDecimal("1.00"), new BigDecimal("5.00")))));

    @Test
    void costoPorMillonDeTokens() {
        // 1M in * 3 + 1M out * 15 = 18.0000
        assertThat(calc.costo("claude-sonnet-4-6", 1_000_000, 1_000_000)).isEqualByComparingTo("18.0000");
        // 500k in * 1 + 200k out * 5 = 0.5 + 1.0 = 1.5000
        assertThat(calc.costo("claude-haiku-4-5", 500_000, 200_000)).isEqualByComparingTo("1.5000");
    }

    @Test
    void redondeaACuatroDecimales() {
        // (10*3 + 20*15)/1e6 = 330/1e6 = 0.00033 -> 0.0003
        assertThat(calc.costo("claude-sonnet-4-6", 10, 20)).isEqualByComparingTo("0.0003");
    }

    @Test
    void modeloSinTarifaCuestaCero() {
        assertThat(calc.costo("modelo-desconocido", 1_000_000, 1_000_000)).isEqualByComparingTo("0.0000");
    }

    @Test
    void emparejaPorPrefijoConSufijoDeFecha() {
        assertThat(calc.costo("claude-sonnet-4-6-20251114", 1_000_000, 0)).isEqualByComparingTo("3.0000");
    }

    @Test
    void exponeUmbralYMoneda() {
        assertThat(calc.umbralMensual()).isEqualByComparingTo("50.00");
        assertThat(calc.moneda()).isEqualTo("USD");
    }
}
