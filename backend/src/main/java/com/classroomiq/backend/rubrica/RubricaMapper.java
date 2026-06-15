package com.classroomiq.backend.rubrica;

import org.mapstruct.Mapper;

import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.dto.CriterioResponse;
import com.classroomiq.backend.rubrica.dto.NivelResponse;
import com.classroomiq.backend.rubrica.dto.RubricaResponse;

@Mapper(componentModel = "spring")
public interface RubricaMapper {

    RubricaResponse toResponse(Rubrica rubrica);

    CriterioResponse toResponse(Criterio criterio);

    NivelResponse toResponse(NivelDesempeno nivel);
}
