package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  Optional<Usuario> findByEmail(String email);

  Boolean existsByEmail(String email);

  @Query("SELECT u FROM Usuario u WHERE u.email = ?1 AND u.tenantId = ?2")
  Optional<Usuario> findByEmailAndTenant(String email, String tenantId);

  @Query("SELECT u FROM Usuario u WHERE u.email = ?1 AND u.activo = true")
  Optional<Usuario> findByEmailAndActive(String email);

  @Modifying
  @Query("UPDATE Usuario u SET u.intentosFallidos = u.intentosFallidos + 1 WHERE u.email = ?1")
  void incrementarIntentosFallidos(String email);

  @Modifying
  @Query("UPDATE Usuario u SET u.intentosFallidos = 0 WHERE u.email = ?1")
  void resetearIntentosFallidos(String email);

  @Modifying
  @Query("UPDATE Usuario u SET u.cuentaBloqueada = true WHERE u.email = ?1")
  void bloquearCuenta(String email);

  List<Usuario> findByTenantId(String tenantId);
}