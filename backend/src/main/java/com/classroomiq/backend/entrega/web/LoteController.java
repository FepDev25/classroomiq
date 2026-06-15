package com.classroomiq.backend.entrega.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.entrega.LoteService;
import com.classroomiq.backend.entrega.dto.LoteRequest;
import com.classroomiq.backend.entrega.dto.LoteResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lotes")
@PreAuthorize("hasRole('DOCENTE')")
public class LoteController {

    private final LoteService lotes;

    public LoteController(LoteService lotes) {
        this.lotes = lotes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoteResponse crear(@Valid @RequestBody LoteRequest request) {
        return lotes.crear(request);
    }

    @GetMapping
    public List<LoteResponse> listar() {
        return lotes.listar();
    }

    @GetMapping("/{id}")
    public LoteResponse obtener(@PathVariable UUID id) {
        return lotes.obtener(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        lotes.eliminar(id);
    }
}
