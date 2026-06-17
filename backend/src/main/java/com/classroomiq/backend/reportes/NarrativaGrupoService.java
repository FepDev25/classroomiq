package com.classroomiq.backend.reportes;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.ReglaNegocioException;
import com.classroomiq.backend.entrega.LoteService;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.metricas.RegistroUsoService;
import com.classroomiq.backend.provider.llm.LlmProvider;
import com.classroomiq.backend.provider.llm.LlmResultado;
import com.classroomiq.backend.provider.llm.LlmSolicitud;
import com.classroomiq.backend.provider.llm.ModeloTier;
import com.classroomiq.backend.reportes.domain.ResumenGrupo;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse;
import com.classroomiq.backend.reportes.repository.ResumenGrupoRepository;

/**
 * Genera el texto narrativo del resumen por grupo (Fase 5, Hito 5). Usa el {@link ModeloTier#ECONOMICO}
 * (Haiku): es un resumen sobre estadísticas ya estructuradas, no un análisis de contenido. La
 * generación es síncrona y explícita (el docente la pide); el texto se persiste porque regenerarlo
 * cuesta una llamada al modelo.
 *
 * <p>Reúsa {@link ResumenGrupoService#obtener} para validar la propiedad del lote y que esté completo
 * (todas las entregas aprobadas), y para obtener las estadísticas que alimentan el prompt — el modelo
 * nunca ve trabajos de estudiantes, solo agregados.
 */
@Service
public class NarrativaGrupoService {

    private final ResumenGrupoService resumenService;
    private final LoteService loteService;
    private final LlmProvider llm;
    private final PromptNarrativa prompt;
    private final ResumenGrupoRepository narrativas;
    private final RegistroUsoService registroUso;

    public NarrativaGrupoService(ResumenGrupoService resumenService, LoteService loteService,
            LlmProvider llm, PromptNarrativa prompt, ResumenGrupoRepository narrativas,
            RegistroUsoService registroUso) {
        this.resumenService = resumenService;
        this.loteService = loteService;
        this.llm = llm;
        this.prompt = prompt;
        this.narrativas = narrativas;
        this.registroUso = registroUso;
    }

    /**
     * Genera (o regenera) la narrativa del lote y la persiste. Devuelve el resumen completo con la
     * narrativa ya incluida.
     */
    @Transactional
    public ResumenGrupoResponse generar(UUID loteId) {
        ResumenGrupoResponse resumen = resumenService.obtener(loteId); // valida propiedad + completitud
        Lote lote = loteService.cargarPropio(loteId);

        LlmResultado resultado = llm.generar(new LlmSolicitud(
                ModeloTier.ECONOMICO, prompt.system(), prompt.usuario(resumen)));
        registroUso.registrarNarrativa(lote.getDocenteId(), loteId, ModeloTier.ECONOMICO, resultado);
        String texto = resultado.texto() == null ? "" : resultado.texto().strip();
        if (texto.isBlank()) {
            throw new ReglaNegocioException("El modelo no devolvió narrativa para el resumen");
        }

        // Upsert in-place (1:1 con el lote): evita el conflicto de borrar/insertar con uq_resgrupo_lote.
        ResumenGrupo entidad = narrativas.findByLoteId(loteId).orElseGet(ResumenGrupo::new);
        entidad.setDocenteId(lote.getDocenteId());
        entidad.setLoteId(loteId);
        entidad.setNarrativa(texto);
        entidad.setModelo(resultado.modelo());
        entidad.setGeneradoAt(Instant.now());
        narrativas.save(entidad);

        return resumenService.obtener(loteId); // ahora incluye la narrativa persistida
    }
}
