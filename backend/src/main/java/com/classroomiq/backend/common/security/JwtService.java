package com.classroomiq.backend.common.security;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.classroomiq.backend.usuario.domain.Usuario;

/**
 * Emite los JWT propios. Los claims llevan el tenant y el rol, que son la base del
 * aislamiento (TenantContext) y de la autorización.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AppJwtProperties properties;

    public JwtService(JwtEncoder jwtEncoder, AppJwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public IssuedToken issue(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(usuario.getId().toString())
                .claim("tenant_id", usuario.getTenantId().toString())
                .claim("rol", usuario.getRol().name())
                .claim("email", usuario.getEmail())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
