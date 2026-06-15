package com.classroomiq.backend.usuario.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.classroomiq.backend.usuario.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** Búsqueda dentro del tenant actual (filtrada por @TenantId). */
    Optional<Usuario> findByEmail(String email);

    /**
     * Login: busca por email SIN filtrar por tenant. El login ocurre antes de conocer el
     * tenant, por eso usa una query nativa, que no aplica el discriminador de @TenantId.
     * El email es único globalmente, así que devuelve a lo sumo un usuario.
     */
    @Query(value = "select * from usuario where email = :email", nativeQuery = true)
    Optional<Usuario> findByEmailAcrossTenants(@Param("email") String email);

    /** Verificación global de unicidad de email (también ignora el tenant). */
    @Query(value = "select exists(select 1 from usuario where email = :email)", nativeQuery = true)
    boolean existsByEmailAcrossTenants(@Param("email") String email);
}
