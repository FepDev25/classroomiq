package com.classroomiq.backend.usuario;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.ConflictoException;
import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.dto.CreateUsuarioRequest;
import com.classroomiq.backend.usuario.dto.UsuarioResponse;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Alta y gestión de cuentas por el admin. El tenant del nuevo usuario lo estampa Hibernate
 * desde el TenantContext (fijado por el TenantFilter a partir del token del admin), de modo que
 * un admin solo crea y gestiona usuarios de su propia institución.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, UsuarioMapper mapper) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional
    public UsuarioResponse crear(CreateUsuarioRequest request) {
        if (request.rol() == Rol.ADMIN) {
            throw new ConflictoException("No se pueden crear cuentas de administrador por este medio");
        }
        if (usuarios.existsByEmailAcrossTenants(request.email())) {
            throw new ConflictoException("El email ya está en uso");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setNombre(request.nombre());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setActivo(true);
        return mapper.toResponse(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public UsuarioResponse cambiarActivo(UUID id, boolean activo) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setActivo(activo);
        return mapper.toResponse(usuarios.save(usuario));
    }
}
