package com.classroomiq.backend.common.tenant;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Portador del tenant activo para el hilo actual. Lo puebla el filtro JWT (Hito 3) en cada
 * request, y los procesos sin request (seed, tests) lo fijan explícitamente con {@link #runWith}.
 *
 * <p>Cuando no hay tenant fijado se devuelve {@link #SIN_TENANT} (fail-closed): las consultas
 * sobre entidades tenant no devuelven filas en lugar de cruzar datos entre instituciones.
 */
public final class TenantContext {

    /** Sentinela para "sin tenant"; ninguna institución real usa el UUID cero. */
    public static final UUID SIN_TENANT = new UUID(0L, 0L);

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(Objects.requireNonNull(tenantId, "tenantId"));
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID getOrSinTenant() {
        UUID current = CURRENT.get();
        return current != null ? current : SIN_TENANT;
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Ejecuta {@code action} con el tenant dado, restaurando el anterior al terminar. */
    public static void runWith(UUID tenantId, Runnable action) {
        UUID previous = CURRENT.get();
        set(tenantId);
        try {
            action.run();
        } finally {
            if (previous != null) {
                CURRENT.set(previous);
            } else {
                CURRENT.remove();
            }
        }
    }
}
