package com.classroomiq.backend.entrega.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.classroomiq.backend.entrega.EntregaService;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.dto.ContenidoEntregaResponse;
import com.classroomiq.backend.entrega.dto.EntregaResponse;

@RestController
@PreAuthorize("hasRole('DOCENTE')")
public class EntregaController {

    private final EntregaService entregas;

    public EntregaController(EntregaService entregas) {
        this.entregas = entregas;
    }

    @PostMapping(path = "/api/lotes/{loteId}/entregas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EntregaResponse subir(
            @PathVariable UUID loteId,
            @RequestParam String identificadorEstudiante,
            @RequestParam TipoEntrega tipo,
            @RequestParam("archivos") List<MultipartFile> archivos) {
        return entregas.subir(loteId, identificadorEstudiante, tipo, archivos);
    }

    @GetMapping("/api/lotes/{loteId}/entregas")
    public List<EntregaResponse> listar(@PathVariable UUID loteId) {
        return entregas.listarPorLote(loteId);
    }

    @GetMapping("/api/entregas/{id}")
    public EntregaResponse obtener(@PathVariable UUID id) {
        return entregas.obtener(id);
    }

    @GetMapping("/api/entregas/{id}/contenido")
    public ContenidoEntregaResponse contenido(@PathVariable UUID id) {
        return entregas.contenido(id);
    }

    @DeleteMapping("/api/entregas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        entregas.eliminar(id);
    }
}
