package com.classroomiq.backend.materia;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.materia.dto.AdminMateriaResponse;
import com.classroomiq.backend.materia.repository.MateriaRepository;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Catálogo de materias de toda la institución para el admin. El aislamiento por tenant lo aplica
 * Hibernate ({@code @TenantId}); a diferencia de {@link MateriaService}, este servicio NO filtra
 * por docente: el admin necesita ver todas las materias para asignarlas a coordinadores. Es solo
 * lectura del catálogo (id, nombre, docente dueño); nunca expone el contenido de las materias.
 */
@Service
public class AdminMateriaService {

    private final MateriaRepository materias;
    private final UsuarioRepository usuarios;

    public AdminMateriaService(MateriaRepository materias, UsuarioRepository usuarios) {
        this.materias = materias;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<AdminMateriaResponse> listarTodas() {
        Map<UUID, String> nombrePorDocente = usuarios.findAll().stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));

        return materias.findAll().stream()
                .map(m -> new AdminMateriaResponse(
                        m.getId(),
                        m.getNombre(),
                        m.getPeriodoAcademico(),
                        m.getDocenteId(),
                        nombrePorDocente.get(m.getDocenteId()),
                        m.isArchivada()))
                .sorted(Comparator
                        .comparing(AdminMateriaResponse::docenteNombre,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(AdminMateriaResponse::nombre,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }
}
