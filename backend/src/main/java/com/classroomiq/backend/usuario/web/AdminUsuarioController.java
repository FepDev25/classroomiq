package com.classroomiq.backend.usuario.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.usuario.UsuarioService;
import com.classroomiq.backend.usuario.dto.CreateUsuarioRequest;
import com.classroomiq.backend.usuario.dto.UsuarioResponse;

import jakarta.validation.Valid;

/** Gestión de cuentas para el admin institucional. */
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final UsuarioService usuarios;

    public AdminUsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CreateUsuarioRequest request) {
        return usuarios.crear(request);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarios.listar();
    }

    @PostMapping("/{id}/activar")
    public UsuarioResponse activar(@PathVariable UUID id) {
        return usuarios.cambiarActivo(id, true);
    }

    @PostMapping("/{id}/desactivar")
    public UsuarioResponse desactivar(@PathVariable UUID id) {
        return usuarios.cambiarActivo(id, false);
    }
}
