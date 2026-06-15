package com.classroomiq.backend.institucion.domain;

import com.classroomiq.backend.common.domain.AbstractAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Institución = tenant. Es la raíz del aislamiento: todas las demás entidades
 * referencian su id como {@code tenant_id}.
 */
@Entity
@Table(name = "institucion")
@Getter
@Setter
public class Institucion extends AbstractAuditableEntity {

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private boolean activa = true;
}
