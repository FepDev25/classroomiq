package com.classroomiq.backend.evaluacion.dto;

import java.util.UUID;

/**
 * Cita textual de evidencia de un criterio, para el resaltado en el panel de la entrega.
 *
 * @param id          id de la cita
 * @param fragmentoId chunk de origen en la entrega (nulo si el fragmento ya no existe)
 * @param textoCitado fragmento exacto citado por el motor
 * @param orden       orden de aparición
 */
public record CitaResponse(UUID id, UUID fragmentoId, String textoCitado, int orden) {
}
