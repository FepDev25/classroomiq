package com.classroomiq.backend.entrega.procesamiento;

import java.util.UUID;

import org.springframework.core.task.TaskDecorator;

import com.classroomiq.backend.common.tenant.TenantContext;

/**
 * Propaga el {@link TenantContext} del hilo que despacha la tarea al hilo del executor, y lo limpia
 * al terminar. Es una <strong>red de seguridad</strong>: la fuente de verdad sigue siendo el
 * {@code tenantId} explícito que recibe el worker (patrón de job auto-describible), robusto ante
 * reinicios o una futura migración a una cola persistente.
 */
class TenantTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        UUID tenant = TenantContext.get().orElse(null);
        return () -> {
            if (tenant != null) {
                TenantContext.set(tenant);
            }
            try {
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
