package com.classroomiq.backend.coordinador;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.coordinador.domain.AsignacionCoordinador;
import com.classroomiq.backend.coordinador.repository.AsignacionCoordinadorRepository;
import com.classroomiq.backend.materia.domain.Materia;
import com.classroomiq.backend.materia.dto.MateriaResponse;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.usuario.domain.Rol;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Gestión de asignaciones materia↔coordinador por el admin (Fase 5, Hito 6). Valida que el usuario
 * destino exista y sea COORDINADOR, y que la materia exista en el tenant. El aislamiento por tenant
 * lo aplica {@code @TenantId}; el admin opera dentro de su institución.
 */
@Service
public class AsignacionCoordinadorService {

    private final AsignacionCoordinadorRepository asignaciones;
    private final UsuarioRepository usuarios;
    private final MateriaRepository materias;

    public AsignacionCoordinadorService(AsignacionCoordinadorRepository asignaciones,
            UsuarioRepository usuarios, MateriaRepository materias) {
        this.asignaciones = asignaciones;
        this.usuarios = usuarios;
        this.materias = materias;
    }

    /** Asigna una materia a un coordinador (idempotente) y devuelve sus materias asignadas. */
    @Transactional
    public List<MateriaResponse> asignar(UUID coordinadorId, UUID materiaId) {
        exigirCoordinador(coordinadorId);
        cargarMateria(materiaId);
        if (!asignaciones.existsByCoordinadorIdAndMateriaId(coordinadorId, materiaId)) {
            AsignacionCoordinador asignacion = new AsignacionCoordinador();
            asignacion.setCoordinadorId(coordinadorId);
            asignacion.setMateriaId(materiaId);
            asignaciones.save(asignacion);
        }
        return listar(coordinadorId);
    }

    /** Quita una materia a un coordinador. 404 si la asignación no existe. */
    @Transactional
    public List<MateriaResponse> desasignar(UUID coordinadorId, UUID materiaId) {
        AsignacionCoordinador asignacion = asignaciones
                .findByCoordinadorIdAndMateriaId(coordinadorId, materiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación no encontrada"));
        asignaciones.delete(asignacion);
        return listar(coordinadorId);
    }

    /** Materias asignadas a un coordinador. */
    @Transactional(readOnly = true)
    public List<MateriaResponse> listar(UUID coordinadorId) {
        exigirCoordinador(coordinadorId);
        return asignaciones.findAllByCoordinadorId(coordinadorId).stream()
                .map(a -> materias.findById(a.getMateriaId()).orElse(null))
                .filter(m -> m != null)
                .map(this::toResponse)
                .toList();
    }

    private void exigirCoordinador(UUID coordinadorId) {
        Usuario usuario = usuarios.findById(coordinadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (usuario.getRol() != Rol.COORDINADOR) {
            throw new ReglaNegocioException("El usuario indicado no es un coordinador");
        }
    }

    private Materia cargarMateria(UUID materiaId) {
        return materias.findById(materiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada"));
    }

    private MateriaResponse toResponse(Materia m) {
        return new MateriaResponse(m.getId(), m.getNombre(), m.getPeriodoAcademico(),
                m.getDescripcion(), m.isArchivada());
    }
}
