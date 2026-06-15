package com.classroomiq.backend.entrega;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.dto.LoteRequest;
import com.classroomiq.backend.entrega.dto.LoteResponse;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.entrega.storage.StorageService;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;

/**
 * Gestión de lotes de entregas del docente autenticado. Verifica que la materia y la rúbrica
 * sean del docente y que la rúbrica pertenezca a la materia antes de crear el lote.
 */
@Service
public class LoteService {

    private final LoteRepository lotes;
    private final MateriaRepository materias;
    private final RubricaRepository rubricas;
    private final StorageService storage;
    private final LoteMapper mapper;
    private final AuthContext auth;

    public LoteService(LoteRepository lotes, MateriaRepository materias, RubricaRepository rubricas,
            StorageService storage, LoteMapper mapper, AuthContext auth) {
        this.lotes = lotes;
        this.materias = materias;
        this.rubricas = rubricas;
        this.storage = storage;
        this.mapper = mapper;
        this.auth = auth;
    }

    @Transactional
    public LoteResponse crear(LoteRequest request) {
        UUID docenteId = auth.requireUserId();
        exigirMateriaPropia(request.materiaId(), docenteId);
        Rubrica rubrica = rubricas.findByIdAndDocenteId(request.rubricaId(), docenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rúbrica no encontrada"));
        if (!rubrica.getMateriaId().equals(request.materiaId())) {
            throw new ReglaNegocioException("La rúbrica no pertenece a la materia indicada");
        }

        Lote lote = new Lote();
        lote.setDocenteId(docenteId);
        lote.setMateriaId(request.materiaId());
        lote.setRubricaId(request.rubricaId());
        lote.setNombre(request.nombre());
        return mapper.toResponse(lotes.save(lote));
    }

    @Transactional(readOnly = true)
    public List<LoteResponse> listar() {
        return lotes.findAllByDocenteId(auth.requireUserId()).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LoteResponse obtener(UUID id) {
        return mapper.toResponse(cargarPropio(id));
    }

    @Transactional
    public void eliminar(UUID id) {
        Lote lote = cargarPropio(id);
        lotes.delete(lote);
        // Borrado en BD por cascada (entregas/archivos/fragmentos); los binarios se borran del disco.
        storage.borrarLote(auth.requireTenantId(), lote.getMateriaId(), lote.getId());
    }

    Lote cargarPropio(UUID id) {
        return lotes.findByIdAndDocenteId(id, auth.requireUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote no encontrado"));
    }

    private void exigirMateriaPropia(UUID materiaId, UUID docenteId) {
        materias.findByIdAndDocenteId(materiaId, docenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada"));
    }
}
