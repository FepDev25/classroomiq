package com.classroomiq.backend.entrega.extraccion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.classroomiq.backend.entrega.domain.ArchivoEntrega;
import com.classroomiq.backend.entrega.storage.StorageService;

/**
 * Orquesta la extracción de texto de una entrega: por cada archivo selecciona el extractor que lo
 * soporta, resuelve su ruta en disco y agrega los segmentos etiquetando el archivo de origen.
 * Una entrega MIXTA se procesa de forma natural: contiene archivos DOCUMENTO y CODIGO juntos.
 */
@Service
public class ServicioExtraccion {

    private final List<ExtractorArchivo> extractores;
    private final StorageService storage;

    public ServicioExtraccion(List<ExtractorArchivo> extractores, StorageService storage) {
        this.extractores = extractores;
        this.storage = storage;
    }

    /** Extrae y agrega los segmentos de todos los archivos de la entrega, en orden. */
    public List<SegmentoTexto> extraer(List<ArchivoEntrega> archivos) {
        List<SegmentoTexto> segmentos = new ArrayList<>();
        for (ArchivoEntrega archivo : archivos) {
            ExtractorArchivo extractor = extractorPara(archivo.getNombreOriginal());
            Path ruta = storage.resolver(archivo.getRutaRelativa());
            for (SegmentoTexto segmento : extractor.extraer(ruta, archivo.getNombreOriginal())) {
                segmentos.add(segmento.conArchivo(archivo.getId()));
            }
        }
        return segmentos;
    }

    private ExtractorArchivo extractorPara(String nombre) {
        return extractores.stream()
                .filter(e -> e.soporta(nombre))
                .findFirst()
                .orElseThrow(() -> new ExtraccionException("No hay extractor para el archivo '" + nombre + "'"));
    }
}
