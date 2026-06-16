package com.classroomiq.backend.reportes;

import org.springframework.stereotype.Component;

import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse.CriterioResumen;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse.NivelConteo;

/**
 * Construye el prompt para la narrativa del resumen por grupo (Fase 5, Hito 5). El modelo recibe las
 * estadísticas ya calculadas (no datos crudos ni trabajos de estudiantes) y redacta un párrafo
 * descriptivo para el docente. Codifica los principios inamovibles: lenguaje objetivo y descriptivo,
 * sin inventar, y el recordatorio de que la nota es responsabilidad del docente.
 */
@Component
public class PromptNarrativa {

    String system() {
        return """
                Eres un asistente que ayuda a un docente universitario a interpretar el desempeño
                agregado de un grupo de estudiantes en una evaluación. Redactas un único párrafo de
                resumen en español, claro y conciso (3 a 5 oraciones), para un informe académico.

                Reglas:
                - Usa lenguaje objetivo y descriptivo, no valorativo ("el grupo calculó X",
                  "hubo dificultad en Y"), nunca juicios sobre estudiantes individuales.
                - Básate EXCLUSIVAMENTE en las estadísticas provistas. No inventes datos ni cifras.
                - Destaca las fortalezas (criterios con mayor desempeño), las dificultades
                  sistemáticas (criterios con menor desempeño) y, si procede, la dispersión.
                - No emitas una calificación ni recomendaciones de nota: la evaluación final es
                  responsabilidad exclusiva del docente.
                - Responde solo con el párrafo, sin encabezados, listas ni comillas.
                """;
    }

    String usuario(ResumenGrupoResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Lote: ").append(r.nombreLote()).append('\n');
        sb.append("Entregas evaluadas: ").append(r.totalEvaluaciones()).append('\n');
        sb.append("Puntaje total de la rúbrica: ").append(fmt(r.puntajeTotalRubrica())).append('\n');
        var e = r.estadisticas();
        sb.append("Notas finales — promedio: ").append(fmt(e.promedio()))
                .append(", mediana: ").append(fmt(e.mediana()))
                .append(", mínima: ").append(fmt(e.minima()))
                .append(", máxima: ").append(fmt(e.maxima())).append('\n');

        sb.append("\nDesempeño por criterio (de mejor a peor):\n");
        for (CriterioResumen c : r.criterios()) {
            sb.append("- ").append(c.nombre()).append(": ");
            if (c.promedioPct() != null) {
                sb.append("promedio ").append(fmt(c.promedio())).append('/').append(fmt(c.puntajeMaximo()))
                        .append(" (").append(fmt(c.promedioPct())).append("%)");
            } else {
                sb.append("sin puntaje registrado");
            }
            sb.append(". Distribución: ").append(distribucion(c)).append('\n');
        }

        if (!r.criteriosDificiles().isEmpty()) {
            sb.append("\nCriterios con mayor dificultad: ")
                    .append(String.join(", ", r.criteriosDificiles())).append('\n');
        }
        return sb.toString();
    }

    private String distribucion(CriterioResumen c) {
        StringBuilder sb = new StringBuilder();
        boolean primero = true;
        for (NivelConteo n : c.distribucion()) {
            if (!primero) {
                sb.append(", ");
            }
            sb.append(n.cantidad()).append(" en ").append(n.nombre());
            primero = false;
        }
        if (c.sinNivel() > 0) {
            sb.append(", ").append(c.sinNivel()).append(" sin nivel");
        }
        return sb.toString();
    }

    private String fmt(double v) {
        return "%.1f".formatted(v);
    }
}
