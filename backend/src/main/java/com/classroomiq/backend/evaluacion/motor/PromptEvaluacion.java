package com.classroomiq.backend.evaluacion.motor;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.classroomiq.backend.entrega.repository.FragmentoSimilar;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;

/**
 * Construye los prompts (sistema + usuario) que evalúan un criterio de la rúbrica. Es el corazón del
 * prompt engineering del proyecto: traduce un criterio + sus niveles + los fragmentos relevantes de
 * la entrega en instrucciones que producen un borrador fundamentado, no una nota.
 *
 * <p>El prompt de sistema codifica los principios inamovibles del CLAUDE.md: citar evidencia
 * textual, declarar cuando el contenido es insuficiente (sin inventar), lenguaje objetivo, y no
 * asignar puntajes fuera del rango del nivel. La salida se exige como un único objeto JSON
 * (contrato {@link EvaluacionLlmRespuesta}).
 */
@Component
public class PromptEvaluacion {

    private static final String SYSTEM = """
            Eres un asistente de evaluación académica universitaria. Tu tarea es preparar un BORRADOR \
            de evaluación de UN criterio de una rúbrica, a partir de fragmentos del trabajo de un \
            estudiante. No eres el evaluador final: un docente revisará, ajustará y aprobará tu \
            borrador. La nota final es responsabilidad exclusiva del docente.

            Reglas estrictas:
            - Fundamenta cada juicio CITANDO fragmentos textuales del trabajo. No inventes evidencia.
            - Si los fragmentos no alcanzan para evaluar el criterio con confianza, dilo en el campo \
            "advertencia" y elige el nivel de forma conservadora; nunca rellenes con suposiciones.
            - Usa lenguaje objetivo y descriptivo ("el trabajo incluye X"), no valorativo ("es excelente").
            - Elige UNO de los niveles ofrecidos por su nombre exacto y asigna un puntaje DENTRO del \
            rango de puntos de ese nivel. No asignes puntajes fuera del rango.
            - Responde EXCLUSIVAMENTE con un único objeto JSON válido, sin texto adicional ni markdown.

            Formato de la respuesta (JSON):
            {
              "nivel": "<nombre exacto de uno de los niveles>",
              "puntaje": <número dentro del rango del nivel>,
              "justificacion": "<explicación objetiva con evidencia del trabajo>",
              "citas": ["<fragmento textual citado del trabajo>", "..."],
              "advertencia": "<aviso si el contenido es insuficiente, o null>"
            }""";

    public String system() {
        return SYSTEM;
    }

    /**
     * Prompt de usuario para evaluar {@code criterio} con los {@code fragmentos} recuperados de la
     * entrega. El criterio debe traer sus niveles cargados.
     */
    public String usuario(Criterio criterio, List<FragmentoSimilar> fragmentos) {
        StringBuilder sb = new StringBuilder();
        sb.append("CRITERIO A EVALUAR\n");
        sb.append("Nombre: ").append(criterio.getNombre()).append('\n');
        if (tiene(criterio.getDescripcion())) {
            sb.append("Descripción: ").append(criterio.getDescripcion()).append('\n');
        }
        sb.append("Puntaje máximo del criterio: ").append(plano(criterio.getPuntajeMaximo())).append('\n');

        sb.append("\nNIVELES DE DESEMPEÑO (elige uno por su nombre):\n");
        for (NivelDesempeno nivel : criterio.getNiveles()) {
            RangoPuntaje rango = RangoPuntaje.de(nivel, criterio.getPuntajeMaximo());
            sb.append("- ").append(nivel.getNombre())
                    .append(" [puntos ").append(plano(rango.min())).append('–').append(plano(rango.max())).append(']');
            if (tiene(nivel.getDescripcion())) {
                sb.append(": ").append(nivel.getDescripcion());
            }
            sb.append('\n');
        }

        sb.append("\nFRAGMENTOS DEL TRABAJO DEL ESTUDIANTE:\n");
        if (fragmentos.isEmpty()) {
            sb.append("(No se recuperaron fragmentos relevantes para este criterio.)\n");
        } else {
            int i = 1;
            for (FragmentoSimilar f : fragmentos) {
                sb.append('[').append(i++).append(']');
                if (tiene(f.getSeccion())) {
                    sb.append(" (").append(f.getSeccion()).append(')');
                }
                sb.append('\n').append(f.getContenido().strip()).append('\n');
            }
        }

        sb.append("\nEvalúa el criterio según la rúbrica y responde solo con el objeto JSON.");
        return sb.toString();
    }

    private static boolean tiene(String s) {
        return s != null && !s.isBlank();
    }

    /** Representación sin ceros de escala innecesarios para el prompt (ej. 20.00 → 20). */
    private static String plano(BigDecimal v) {
        return v == null ? "?" : v.stripTrailingZeros().toPlainString();
    }
}
