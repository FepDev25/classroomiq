package com.classroomiq.backend.usuario;

import org.mapstruct.Mapper;

import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.dto.UsuarioResponse;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponse toResponse(Usuario usuario);
}
