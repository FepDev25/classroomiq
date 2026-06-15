package com.classroomiq.backend.common.tenant;

import java.util.UUID;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Le dice a Hibernate cuál es el tenant activo, leyéndolo del {@link TenantContext}.
 * Se registra en Hibernate vía {@link MultiTenancyConfig}.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.getOrSinTenant();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
