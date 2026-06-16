package com.classroomiq.backend.entrega.repository;

import java.util.UUID;

/**
 * Proyección de un fragmento recuperado por similitud semántica, con su distancia coseno respecto
 * al vector de consulta. La usan el retrieval por criterio del motor de evaluación (Fase 4) y la
 * detección de similitud entre entregas (Fase 5).
 *
 * <p>{@code distancia} es el resultado del operador {@code <=>} de pgvector: 0 = idéntico,
 * 2 = opuesto. Con vectores normalizados L2 equivale a {@code 1 - similitudCoseno}, por lo que un
 * valor menor indica mayor relevancia. Trae el contenido y la procedencia (archivo, sección,
 * líneas) necesarios para construir el prompt y para el resaltado en la pantalla de revisión.
 */
public interface FragmentoSimilar {

    UUID getId();

    UUID getArchivoId();

    String getContenido();

    String getSeccion();

    Integer getLineaInicio();

    Integer getLineaFin();

    double getDistancia();
}
