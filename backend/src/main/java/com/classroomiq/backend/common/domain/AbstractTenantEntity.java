package com.classroomiq.backend.common.domain;

import java.util.UUID;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

/**
 * Base de las entidades pertenecientes a un tenant (institución).
 *
 * <p>{@code @TenantId} activa la multi-tenancy por discriminador de Hibernate: el valor se
 * estampa automáticamente en cada INSERT a partir del {@code CurrentTenantIdentifierResolver},
 * y Hibernate añade {@code tenant_id = :actual} a cada SELECT/UPDATE/DELETE (incluido findById).
 * No se debe asignar {@code tenantId} a mano: lo gestiona Hibernate desde el TenantContext.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractTenantEntity extends AbstractAuditableEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
