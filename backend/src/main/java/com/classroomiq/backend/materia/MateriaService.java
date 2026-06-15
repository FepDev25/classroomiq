package com.classroomiq.backend.materia;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.dto.MateriaRequest;
import com.classroomiq.backend.materia.dto.MateriaResponse;
import com.classroomiq.backend.materia.repository.MateriaRepository;

/**
 * Gestión de materias del docente autenticado. El aislamiento por tenant lo aplica Hibernate;
 * el aislamiento por docente se aplica acá usando el id del usuario del {@link AuthContext}.
 */
@Service
public class MateriaService {

    private final MateriaRepository materias;
    private final MateriaMapper mapper;
    private final AuthContext auth;

    public MateriaService(MateriaRepository materias, MateriaMapper mapper, AuthContext auth) {
        this.materias = materias;
        this.mapper = mapper;
        this.auth = auth;
    }

    @Transactional
    public MateriaResponse crear(MateriaRequest request) {
        Materia materia = new Materia();
        materia.setDocenteId(auth.requireUserId());
        aplicar(materia, request);
        return mapper.toResponse(materias.save(materia));
    }

    @Transactional(readOnly = true)
    public List<MateriaResponse> listar() {
        return materias.findAllByDocenteId(auth.requireUserId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MateriaResponse obtener(UUID id) {
        return mapper.toResponse(cargarPropia(id));
    }

    @Transactional
    public MateriaResponse actualizar(UUID id, MateriaRequest request) {
        Materia materia = cargarPropia(id);
        aplicar(materia, request);
        return mapper.toResponse(materias.save(materia));
    }

    @Transactional
    public MateriaResponse archivar(UUID id) {
        Materia materia = cargarPropia(id);
        materia.setArchivada(true);
        return mapper.toResponse(materias.save(materia));
    }

    private Materia cargarPropia(UUID id) {
        return materias.findByIdAndDocenteId(id, auth.requireUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada"));
    }

    private void aplicar(Materia materia, MateriaRequest request) {
        materia.setNombre(request.nombre());
        materia.setPeriodoAcademico(request.periodoAcademico());
        materia.setDescripcion(request.descripcion());
    }
}
