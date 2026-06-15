package com.classroomiq.backend.rubrica;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.NivelDesempeno;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.dto.CriterioRequest;
import com.classroomiq.backend.rubrica.dto.NivelRequest;
import com.classroomiq.backend.rubrica.dto.RubricaRequest;
import com.classroomiq.backend.rubrica.dto.RubricaResponse;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;

/**
 * CRUD del agregado rúbrica (con criterios y niveles anidados). Verifica la propiedad de la
 * materia y de la rúbrica por docente, y valida la coherencia de puntajes antes de persistir.
 */
@Service
public class RubricaService {

    private final RubricaRepository rubricas;
    private final MateriaRepository materias;
    private final RubricaMapper mapper;
    private final RubricaValidator validator;
    private final AuthContext auth;

    public RubricaService(RubricaRepository rubricas, MateriaRepository materias, RubricaMapper mapper,
            RubricaValidator validator, AuthContext auth) {
        this.rubricas = rubricas;
        this.materias = materias;
        this.mapper = mapper;
        this.validator = validator;
        this.auth = auth;
    }

    @Transactional
    public RubricaResponse crear(UUID materiaId, RubricaRequest request) {
        UUID docenteId = auth.requireUserId();
        exigirMateriaPropia(materiaId, docenteId);
        validator.validar(request);

        Rubrica rubrica = new Rubrica();
        rubrica.setMateriaId(materiaId);
        rubrica.setDocenteId(docenteId);
        aplicar(rubrica, request);
        return mapper.toResponse(rubricas.save(rubrica));
    }

    @Transactional(readOnly = true)
    public List<RubricaResponse> listarPorMateria(UUID materiaId) {
        exigirMateriaPropia(materiaId, auth.requireUserId());
        return rubricas.findAllByMateriaId(materiaId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RubricaResponse obtener(UUID id) {
        return mapper.toResponse(cargarPropia(id));
    }

    @Transactional
    public RubricaResponse actualizar(UUID id, RubricaRequest request) {
        validator.validar(request);
        Rubrica rubrica = cargarPropia(id);
        aplicar(rubrica, request);
        return mapper.toResponse(rubricas.save(rubrica));
    }

    @Transactional
    public void eliminar(UUID id) {
        rubricas.delete(cargarPropia(id));
    }

    private void exigirMateriaPropia(UUID materiaId, UUID docenteId) {
        materias.findByIdAndDocenteId(materiaId, docenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada"));
    }

    private Rubrica cargarPropia(UUID id) {
        return rubricas.findByIdAndDocenteId(id, auth.requireUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rúbrica no encontrada"));
    }

    /** Vuelca el request al agregado, reconstruyendo criterios y niveles (también sirve para update). */
    private void aplicar(Rubrica rubrica, RubricaRequest request) {
        rubrica.setNombre(request.nombre());
        rubrica.setDescripcion(request.descripcion());
        rubrica.setPuntajeTotal(request.puntajeTotal());
        rubrica.setModoTotal(request.modoTotal());
        rubrica.getCriterios().clear();

        int ordenCriterio = 0;
        for (CriterioRequest criterioRequest : request.criterios()) {
            Criterio criterio = new Criterio();
            criterio.setNombre(criterioRequest.nombre());
            criterio.setDescripcion(criterioRequest.descripcion());
            criterio.setPuntajeMaximo(criterioRequest.puntajeMaximo());
            criterio.setEvaluablePorContenido(
                    criterioRequest.evaluablePorContenido() == null || criterioRequest.evaluablePorContenido());
            criterio.setOrden(ordenCriterio++);

            int ordenNivel = 0;
            for (NivelRequest nivelRequest : criterioRequest.niveles()) {
                NivelDesempeno nivel = new NivelDesempeno();
                nivel.setNombre(nivelRequest.nombre());
                nivel.setDescripcion(nivelRequest.descripcion());
                nivel.setTipoPuntaje(nivelRequest.tipoPuntaje());
                nivel.setPuntajeMin(nivelRequest.puntajeMin());
                nivel.setPuntajeMax(nivelRequest.puntajeMax());
                nivel.setPuntajeValor(nivelRequest.puntajeValor());
                nivel.setPctMin(nivelRequest.pctMin());
                nivel.setPctMax(nivelRequest.pctMax());
                nivel.setOrden(ordenNivel++);
                criterio.addNivel(nivel);
            }
            rubrica.addCriterio(criterio);
        }
    }
}
