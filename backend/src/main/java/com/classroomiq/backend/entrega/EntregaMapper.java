package com.classroomiq.backend.entrega;

import java.util.List;

import org.mapstruct.Mapper;

import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.dto.ArchivoResponse;
import com.classroomiq.backend.entrega.dto.EntregaResponse;

@Mapper(componentModel = "spring")
public interface EntregaMapper {

    EntregaResponse toResponse(Entrega entrega);

    ArchivoResponse toResponse(ArchivoEntrega archivo);

    List<ArchivoResponse> toArchivoResponses(List<ArchivoEntrega> archivos);
}
