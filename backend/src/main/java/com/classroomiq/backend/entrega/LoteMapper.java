package com.classroomiq.backend.entrega;

import org.mapstruct.Mapper;

import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.dto.LoteResponse;

@Mapper(componentModel = "spring")
public interface LoteMapper {

    LoteResponse toResponse(Lote lote);
}
