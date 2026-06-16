package com.classroomiq.backend.reportes.dto;

import java.util.List;
import java.util.UUID;

/**
 * Resumen por grupo de un lote (Fase 5, sección 6): estadísticas de las notas finales, análisis por
 * criterio (promedio y distribución por nivel = mapa de dominio grupal), los criterios de mayor
 * dificultad, y un texto narrativo generado por LLM (Hito 5; {@code null} hasta entonces).
 *
 * <p>Se computa sobre las evaluaciones APROBADA del lote, que están congeladas.
 */
public record ResumenGrupoResponse(
        UUID loteId,
        String nombreLote,
        int totalEvaluaciones,
        double puntajeTotalRubrica,
        EstadisticasNotas estadisticas,
        List<CriterioResumen> criterios,
        List<String> criteriosDificiles,
        String narrativa) {

    /** Estadísticas generales de las notas finales del lote, con histograma por rangos. */
    public record EstadisticasNotas(
            double promedio,
            double mediana,
            double minima,
            double maxima,
            List<RangoNota> histograma) {
    }

    /** Un rango del histograma de notas y cuántas entregas cayeron en él. */
    public record RangoNota(String etiqueta, double min, double max, int cantidad) {
    }

    /**
     * Desempeño grupal en un criterio. {@code promedio}/{@code promedioPct} son {@code null} si
     * ningún integrante del grupo tuvo puntaje en ese criterio (ej. criterio de juicio docente sin
     * llenar). {@code distribucion} es el mapa de dominio: cuántos alcanzaron cada nivel.
     */
    public record CriterioResumen(
            UUID criterioId,
            String nombre,
            double puntajeMaximo,
            Double promedio,
            Double promedioPct,
            int evaluados,
            List<NivelConteo> distribucion,
            int sinNivel) {
    }

    /** Cuántas entregas del grupo alcanzaron un nivel de desempeño, y qué porcentaje representan. */
    public record NivelConteo(UUID nivelId, String nombre, int cantidad, double porcentaje) {
    }
}
