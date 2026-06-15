package com.classroomiq.backend.common.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el {@link TenantIdentifierResolver} en Hibernate. Junto con {@code @TenantId}
 * en las entidades, esto habilita la multi-tenancy por discriminador (filtrado y estampado
 * automático de {@code tenant_id}).
 */
@Configuration
public class MultiTenancyConfig {

    @Bean
    HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer(
            CurrentTenantIdentifierResolver<?> resolver) {
        return properties ->
                properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
