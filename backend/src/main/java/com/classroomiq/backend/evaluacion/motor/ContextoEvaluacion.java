package com.classroomiq.backend.evaluacion.motor;

import java.util.List;
import java.util.UUID;

import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.ModoTotal;

/**
 * Datos que el motor necesita para evaluar una entrega, leídos en una sola transacción de solo
 * lectura por {@link ContextoEvaluacionLoader}. Los {@code criterios} vienen con sus niveles ya
 * inicializados para poder navegarlos fuera de sesión durante las (lentas) llamadas al LLM.
 *
 * @param entregaId  entrega a evaluar
 * @param docenteId  propietario (para estampar la evaluación y el scoping por docente)
 * @param rubricaId  rúbrica aplicada (vía el lote de la entrega)
 * @param modoTotal  cómo se calcula el total proyectado a partir de los criterios
 * @param criterios  criterios de la rúbrica, en orden, con niveles inicializados
 */
public record ContextoEvaluacion(
        UUID entregaId,
        UUID docenteId,
        UUID rubricaId,
        ModoTotal modoTotal,
        List<Criterio> criterios) {
}
