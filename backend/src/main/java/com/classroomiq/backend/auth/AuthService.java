package com.classroomiq.backend.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.classroomiq.backend.auth.dto.LoginRequest;
import com.classroomiq.backend.auth.dto.TokenResponse;
import com.classroomiq.backend.common.security.JwtService;
import com.classroomiq.backend.common.tenant.TenantContext;
import com.classroomiq.backend.usuario.domain.Usuario;
import com.classroomiq.backend.usuario.repository.UsuarioRepository;

/**
 * Autentica por email/contraseña y emite el JWT. No es {@code @Transactional}: el lookup global
 * (nativo) y la actualización del último acceso corren en sesiones separadas, de modo que la
 * actualización se haga con el tenant del usuario ya fijado en el contexto.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByEmailAcrossTenants(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!usuario.isActivo()) {
            throw new DisabledException("La cuenta está inactiva");
        }
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        registrarUltimoAcceso(usuario);

        JwtService.IssuedToken token = jwtService.issue(usuario);
        long expiresIn = Duration.between(Instant.now(), token.expiresAt()).getSeconds();
        return new TokenResponse(token.value(), "Bearer", expiresIn, usuario.getId(), usuario.getRol());
    }

    private void registrarUltimoAcceso(Usuario usuario) {
        // El usuario se cargó sin tenant (query nativa); fijamos su tenant para que el UPDATE
        // quede correctamente filtrado por el discriminador.
        TenantContext.set(usuario.getTenantId());
        try {
            usuario.setUltimoAcceso(Instant.now());
            usuarios.save(usuario);
        } finally {
            TenantContext.clear();
        }
    }
}
