package com.classroomiq.backend.entrega.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Almacenamiento local de los archivos de entregas, organizado por
 * {@code base/tenant/materia/lote/entrega}. Los archivos son sensibles y no salen del servidor.
 *
 * <p>Escribe de forma atómica (archivo temporal + {@code move}), calcula el SHA-256 en streaming
 * y sanea el nombre original para evitar path traversal. La ruta devuelta es relativa a la base,
 * que es lo que se persiste en {@code archivo_entrega.ruta_relativa}.
 */
@Service
public class StorageService {

    private final Path base;

    public StorageService(StorageProperties properties) {
        this.base = Path.of(properties.basePath()).toAbsolutePath().normalize();
    }

    /** Metadatos del archivo ya almacenado. */
    public record StoredFile(String rutaRelativa, String hashSha256, long tamanoBytes, String mimeType) {
    }

    public StoredFile guardar(UUID tenantId, UUID materiaId, UUID loteId, UUID entregaId, int orden,
            MultipartFile archivo) {
        Path dir = directorioEntrega(tenantId, materiaId, loteId, entregaId);
        String nombre = orden + "__" + sanitizar(archivo.getOriginalFilename());
        MessageDigest md = sha256();
        try {
            Files.createDirectories(dir);
            Path destino = dir.resolve(nombre);
            Path temp = Files.createTempFile(dir, ".tmp-", null);
            try (InputStream in = archivo.getInputStream();
                    DigestInputStream din = new DigestInputStream(in, md);
                    OutputStream out = Files.newOutputStream(temp)) {
                din.transferTo(out);
            } catch (IOException e) {
                Files.deleteIfExists(temp);
                throw e;
            }
            Files.move(temp, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            String rutaRelativa = base.relativize(destino).toString();
            return new StoredFile(rutaRelativa, HexFormat.of().formatHex(md.digest()),
                    Files.size(destino), archivo.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo '" + nombre + "'", e);
        }
    }

    /** Ruta absoluta de un archivo almacenado (para la extracción de texto del Hito 3). */
    public Path resolver(String rutaRelativa) {
        Path resuelta = base.resolve(rutaRelativa).normalize();
        if (!resuelta.startsWith(base)) {
            throw new IllegalArgumentException("Ruta fuera del almacenamiento: " + rutaRelativa);
        }
        return resuelta;
    }

    public void borrarEntrega(UUID tenantId, UUID materiaId, UUID loteId, UUID entregaId) {
        borrarRecursivo(directorioEntrega(tenantId, materiaId, loteId, entregaId));
    }

    public void borrarLote(UUID tenantId, UUID materiaId, UUID loteId) {
        borrarRecursivo(base.resolve(Path.of(tenantId.toString(), materiaId.toString(), loteId.toString())));
    }

    private Path directorioEntrega(UUID tenantId, UUID materiaId, UUID loteId, UUID entregaId) {
        return base.resolve(Path.of(tenantId.toString(), materiaId.toString(),
                loteId.toString(), entregaId.toString()));
    }

    private void borrarRecursivo(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (var rutas = Files.walk(dir)) {
            rutas.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException("No se pudo borrar " + p, e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar el directorio " + dir, e);
        }
    }

    private static String sanitizar(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "archivo";
        }
        String soloNombre = Path.of(nombreOriginal).getFileName().toString();
        return soloNombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
