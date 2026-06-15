package com.classroomiq.backend.materia.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.materia.MateriaService;
import com.classroomiq.backend.materia.dto.MateriaRequest;
import com.classroomiq.backend.materia.dto.MateriaResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/materias")
@PreAuthorize("hasRole('DOCENTE')")
public class MateriaController {

    private final MateriaService materias;

    public MateriaController(MateriaService materias) {
        this.materias = materias;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MateriaResponse crear(@Valid @RequestBody MateriaRequest request) {
        return materias.crear(request);
    }

    @GetMapping
    public List<MateriaResponse> listar() {
        return materias.listar();
    }

    @GetMapping("/{id}")
    public MateriaResponse obtener(@PathVariable UUID id) {
        return materias.obtener(id);
    }

    @PutMapping("/{id}")
    public MateriaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody MateriaRequest request) {
        return materias.actualizar(id, request);
    }

    @PostMapping("/{id}/archivar")
    public MateriaResponse archivar(@PathVariable UUID id) {
        return materias.archivar(id);
    }
}
