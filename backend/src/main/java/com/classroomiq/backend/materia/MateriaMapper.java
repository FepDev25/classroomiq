package com.classroomiq.backend.materia;

import org.mapstruct.Mapper;

import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.dto.MateriaResponse;

@Mapper(componentModel = "spring")
public interface MateriaMapper {

    MateriaResponse toResponse(Materia materia);
}
