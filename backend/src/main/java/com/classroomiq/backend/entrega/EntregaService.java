package com.classroomiq.backend.entrega;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.common.security.AuthContext;
import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.domain.RolArchivo;
import com.classroomiq.backend.entrega.domain.TipoEntrega;
import com.classroomiq.backend.entrega.dto.EntregaResponse;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.storage.StorageService;

/**
 * Subida y consulta de entregas dentro de un lote. Al subir, clasifica cada archivo por su
 * extensión, valida que el conjunto sea coherente con el tipo declarado, persiste la entrega en
 * estado {@code PENDIENTE} y almacena los binarios en disco. El procesamiento se dispara en el
 * Hito 5.
 */
@Service
public class EntregaService {

    private final EntregaRepository entregas;
    private final LoteService lotes;
    private final StorageService storage;
    private final EntregaMapper mapper;
    private final AuthContext auth;

    public EntregaService(EntregaRepository entregas, LoteService lotes, StorageService storage,
            EntregaMapper mapper, AuthContext auth) {
        this.entregas = entregas;
        this.lotes = lotes;
        this.storage = storage;
        this.mapper = mapper;
        this.auth = auth;
    }

    @Transactional
    public EntregaResponse subir(UUID loteId, String identificadorEstudiante, TipoEntrega tipo,
            List<MultipartFile> archivos) {
        Lote lote = lotes.cargarPropio(loteId);
        List<RolArchivo> roles = clasificarYValidar(tipo, archivos);

        Entrega entrega = new Entrega();
        entrega.setLoteId(lote.getId());
        entrega.setDocenteId(lote.getDocenteId());
        entrega.setMateriaId(lote.getMateriaId());
        entrega.setIdentificadorEstudiante(identificadorEstudiante);
        entrega.setTipo(tipo);
        // Primer save: necesitamos el id de la entrega para la ruta en disco.
        Entrega persistida = entregas.save(entrega);

        UUID tenantId = auth.requireTenantId();
        try {
            for (int i = 0; i < archivos.size(); i++) {
                StorageService.StoredFile stored = storage.guardar(tenantId, lote.getMateriaId(),
                        lote.getId(), persistida.getId(), i, archivos.get(i));
                ArchivoEntrega archivo = new ArchivoEntrega();
                archivo.setNombreOriginal(nombreOriginal(archivos.get(i)));
                archivo.setRutaRelativa(stored.rutaRelativa());
                archivo.setMimeType(stored.mimeType());
                archivo.setTamanoBytes(stored.tamanoBytes());
                archivo.setHashSha256(stored.hashSha256());
                archivo.setRol(roles.get(i));
                archivo.setOrden(i);
                persistida.addArchivo(archivo);
            }
        } catch (RuntimeException e) {
            // El rollback deshace la BD; limpiamos los binarios ya escritos para no dejar huérfanos.
            storage.borrarEntrega(tenantId, lote.getMateriaId(), lote.getId(), persistida.getId());
            throw e;
        }
        return mapper.toResponse(entregas.save(persistida));
    }

    @Transactional(readOnly = true)
    public List<EntregaResponse> listarPorLote(UUID loteId) {
        lotes.cargarPropio(loteId);
        return entregas.findAllByLoteId(loteId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EntregaResponse obtener(UUID id) {
        return mapper.toResponse(cargarPropia(id));
    }

    @Transactional
    public void eliminar(UUID id) {
        Entrega entrega = cargarPropia(id);
        entregas.delete(entrega);
        storage.borrarEntrega(auth.requireTenantId(), entrega.getMateriaId(),
                entrega.getLoteId(), entrega.getId());
    }

    private Entrega cargarPropia(UUID id) {
        return entregas.findByIdAndDocenteId(id, auth.requireUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega no encontrada"));
    }

    private List<RolArchivo> clasificarYValidar(TipoEntrega tipo, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty() || archivos.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ReglaNegocioException("La entrega debe incluir al menos un archivo");
        }
        List<RolArchivo> roles = archivos.stream().map(this::clasificar).toList();
        boolean hayDocumento = roles.contains(RolArchivo.DOCUMENTO);
        boolean hayCodigo = roles.contains(RolArchivo.CODIGO);
        switch (tipo) {
            case DOCUMENTO -> {
                if (hayCodigo) {
                    throw new ReglaNegocioException("Una entrega DOCUMENTO solo admite documentos (PDF/DOCX)");
                }
            }
            case CODIGO -> {
                if (hayDocumento) {
                    throw new ReglaNegocioException("Una entrega CODIGO solo admite un ZIP de código");
                }
            }
            case MIXTA -> {
                if (!hayDocumento || !hayCodigo) {
                    throw new ReglaNegocioException(
                            "Una entrega MIXTA requiere al menos un documento y un ZIP de código");
                }
            }
        }
        return roles;
    }

    private RolArchivo clasificar(MultipartFile archivo) {
        String nombre = nombreOriginal(archivo).toLowerCase();
        if (nombre.endsWith(".pdf") || nombre.endsWith(".docx") || nombre.endsWith(".doc")) {
            return RolArchivo.DOCUMENTO;
        }
        if (nombre.endsWith(".zip")) {
            return RolArchivo.CODIGO;
        }
        throw new ReglaNegocioException("Tipo de archivo no soportado: " + nombre);
    }

    private static String nombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        return (nombre == null || nombre.isBlank()) ? "archivo" : nombre;
    }
}
