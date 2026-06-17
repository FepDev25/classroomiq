package com.classroomiq.backend.coordinador;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.coordinador.repository.AsignacionCoordinadorRepository;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.dto.LoteResponse;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.dto.MateriaResponse;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.reportes.ResumenGrupoService;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse;

/**
 * Acceso de SOLO LECTURA del coordinador a los reportes agregados de sus materias asignadas (Fase 5,
 * sección 7). El coordinador ve materias → lotes → resumen por grupo (estadísticas + narrativa);
 * nunca evaluaciones, trabajos individuales ni el reporte de similitud (esos exponen entregas
 * concretas). El acceso se acota por la asignación del coordinador autenticado, no por dueño docente.
 *
 * <p>Para evitar filtrar la existencia de recursos ajenos, lo no asignado responde 404 (igual que el
 * scoping por docente). El aislamiento por tenant lo aplica {@code @TenantId}.
 */
@Service
public class CoordinadorReporteService {

    private final AsignacionCoordinadorRepository asignaciones;
    private final MateriaRepository materias;
    private final LoteRepository lotes;
    private final ResumenGrupoService resumenService;
    private final AuthContext auth;

    public CoordinadorReporteService(AsignacionCoordinadorRepository asignaciones,
            MateriaRepository materias, LoteRepository lotes, ResumenGrupoService resumenService,
            AuthContext auth) {
        this.asignaciones = asignaciones;
        this.materias = materias;
        this.lotes = lotes;
        this.resumenService = resumenService;
        this.auth = auth;
    }

    /** Materias asignadas al coordinador autenticado. */
    @Transactional(readOnly = true)
    public List<MateriaResponse> materiasAsignadas() {
        UUID coordinadorId = auth.requireUserId();
        return asignaciones.findAllByCoordinadorId(coordinadorId).stream()
                .map(a -> materias.findById(a.getMateriaId()).orElse(null))
                .filter(m -> m != null)
                .map(this::toMateriaResponse)
                .toList();
    }

    /** Lotes de una materia asignada al coordinador. 404 si la materia no le fue asignada. */
    @Transactional(readOnly = true)
    public List<LoteResponse> lotesDeMateria(UUID materiaId) {
        exigirMateriaAsignada(materiaId);
        return lotes.findAllByMateriaId(materiaId).stream()
                .map(this::toLoteResponse)
                .toList();
    }

    /** Resumen por grupo de un lote de una materia asignada. 404 si el lote/materia no le fue asignado. */
    @Transactional(readOnly = true)
    public ResumenGrupoResponse resumenDeLote(UUID loteId) {
        Lote lote = lotes.findById(loteId) // filtrado por tenant; el coordinador no es el dueño docente
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote no encontrado"));
        exigirMateriaAsignada(lote.getMateriaId());
        return resumenService.resumir(lote);
    }

    private void exigirMateriaAsignada(UUID materiaId) {
        if (!asignaciones.existsByCoordinadorIdAndMateriaId(auth.requireUserId(), materiaId)) {
            throw new RecursoNoEncontradoException("Materia no encontrada");
        }
    }

    private MateriaResponse toMateriaResponse(Materia m) {
        return new MateriaResponse(m.getId(), m.getNombre(), m.getPeriodoAcademico(),
                m.getDescripcion(), m.isArchivada());
    }

    private LoteResponse toLoteResponse(Lote l) {
        return new LoteResponse(l.getId(), l.getMateriaId(), l.getRubricaId(), l.getNombre(), l.getEstado());
    }
}
