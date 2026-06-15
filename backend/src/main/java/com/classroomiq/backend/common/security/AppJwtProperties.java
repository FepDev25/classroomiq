package com.classroomiq.backend.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del JWT propio (prefijo {@code app.jwt} en application.yml).
 *
 * @param issuer         emisor del token
 * @param secret         secreto HMAC (mínimo 32 bytes para HS256)
 * @param accessTokenTtl vigencia del access token (ej. PT1H)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record AppJwtProperties(String issuer, String secret, Duration accessTokenTtl) {
}
