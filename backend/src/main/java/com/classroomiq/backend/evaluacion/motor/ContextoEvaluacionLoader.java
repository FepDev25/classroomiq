package com.classroomiq.backend.evaluacion.motor;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.classroomiq.backend.common.error.RecursoNoEncontradoException;
import com.classroomiq.backend.entrega.domain.Entrega;
import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.Lote;
import com.classroomiq.backend.entrega.repository.EntregaRepository;
import com.classroomiq.backend.entrega.repository.LoteRepository;
import com.classroomiq.backend.rubrica.domain.Criterio;
import com.classroomiq.backend.rubrica.domain.Rubrica;
import com.classroomiq.backend.rubrica.repository.RubricaRepository;

import com.classroomiq.backend.common.error.ReglaNegocioException;

/**
 * Carga, en una única transacción de solo lectura, el contexto que el motor necesita para evaluar
 * una entrega: la entrega, la rúbrica de su lote y los criterios con sus niveles inicializados.
 *
 * <p>Vive aparte del motor para que el {@code @Transactional} se aplique vía proxy (evita la
 * autoinvocación) y para que el motor pueda hacer las llamadas al LLM <em>fuera</em> de toda
 * transacción, sin retener una conexión durante el HTTP. El aislamiento por tenant lo garantiza
 * {@code @TenantId} sobre los repositorios.
 */
@Component
public class ContextoEvaluacionLoader {

    private final EntregaRepository entregas;
    private final LoteRepository lotes;
    private final RubricaRepository rubricas;

    public ContextoEvaluacionLoader(EntregaRepository entregas, LoteRepository lotes,
            RubricaRepository rubricas) {
        this.entregas = entregas;
        this.lotes = lotes;
        this.rubricas = rubricas;
    }

    @Transactional(readOnly = true)
    public ContextoEvaluacion cargar(UUID entregaId) {
        Entrega entrega = entregas.findById(entregaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega no encontrada"));
        if (entrega.getEstado() != EstadoEntrega.LISTO) {
            throw new ReglaNegocioException(
                    "La entrega debe estar indexada (estado LISTO) antes de evaluarse; está en "
                            + entrega.getEstado());
        }

        Lote lote = lotes.findById(entrega.getLoteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Lote no encontrado"));
        Rubrica rubrica = rubricas.findById(lote.getRubricaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rúbrica no encontrada"));

        // Inicializa el agregado (criterios y, dentro de cada uno, sus niveles) para navegarlo
        // ya detached durante las llamadas al LLM.
        List<Criterio> criterios = rubrica.getCriterios();
        criterios.forEach(c -> c.getNiveles().size());

        return new ContextoEvaluacion(entrega.getId(), entrega.getDocenteId(), rubrica.getId(),
                rubrica.getModoTotal(), List.copyOf(criterios));
    }
}
