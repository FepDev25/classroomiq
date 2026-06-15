package com.classroomiq.backend.rubrica.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.rubrica.RubricaService;
import com.classroomiq.backend.rubrica.dto.RubricaRequest;
import com.classroomiq.backend.rubrica.dto.RubricaResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('DOCENTE')")
public class RubricaController {

    private final RubricaService rubricas;

    public RubricaController(RubricaService rubricas) {
        this.rubricas = rubricas;
    }

    @PostMapping("/materias/{materiaId}/rubricas")
    @ResponseStatus(HttpStatus.CREATED)
    public RubricaResponse crear(@PathVariable UUID materiaId, @Valid @RequestBody RubricaRequest request) {
        return rubricas.crear(materiaId, request);
    }

    @GetMapping("/materias/{materiaId}/rubricas")
    public List<RubricaResponse> listarPorMateria(@PathVariable UUID materiaId) {
        return rubricas.listarPorMateria(materiaId);
    }

    @GetMapping("/rubricas/{id}")
    public RubricaResponse obtener(@PathVariable UUID id) {
        return rubricas.obtener(id);
    }

    @PutMapping("/rubricas/{id}")
    public RubricaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody RubricaRequest request) {
        return rubricas.actualizar(id, request);
    }

    @DeleteMapping("/rubricas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        rubricas.eliminar(id);
    }
}
