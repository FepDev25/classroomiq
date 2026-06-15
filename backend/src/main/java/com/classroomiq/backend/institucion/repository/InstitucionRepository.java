package com.classroomiq.backend.institucion.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.classroomiq.backend.institucion.domain.Institucion;

public interface InstitucionRepository extends JpaRepository<Institucion, UUID> {
}
