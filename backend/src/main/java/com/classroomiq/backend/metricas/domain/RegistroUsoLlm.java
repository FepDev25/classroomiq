package com.classroomiq.backend.metricas.domain;

import java.util.UUID;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;
import com.classroomiq.backend.provider.llm.ModeloTier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Una llamada al LLM con los tokens que consumió (Fase 6, Hito 0): el libro mayor de uso del que
 * el portal admin deriva uso y costo estimado por docente y por mes. Es inmutable una vez escrito
 * (se inserta, nunca se actualiza); {@code createdAt} de la superclase auditable es el instante del
 * consumo y la clave de la agregación mensual.
 *
 * <p>El costo no se guarda: se calcula on-read desde la tarifa configurable, porque los tokens son
 * el hecho inmutable y el precio es una estimación que puede cambiar. {@code docenteId} replica el
 * propietario para agrupar por docente; {@code modelo} es la clave para la tarifa de costo.
 * {@code entregaId}/{@code loteId} son trazabilidad opcional (se sueltan si se borra el origen).
 */
@Entity
@Table(name = "registro_uso_llm")
@Getter
@Setter
public class RegistroUsoLlm extends AbstractTenantEntity {

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperacionLlm operacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModeloTier tier;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    @Column(name = "entrega_id")
    private UUID entregaId;

    @Column(name = "lote_id")
    private UUID loteId;
}
