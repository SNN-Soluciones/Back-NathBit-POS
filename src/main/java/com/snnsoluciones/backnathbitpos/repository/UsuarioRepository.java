package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  Optional<Usuario> findByEmail(String email);

  Boolean existsByEmail(String email);

  @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.tenantId = :tenantId")
  Optional<Usuario> findByEmailAndTenant(@Param("email") String email, @Param("tenantId") String tenantId);

  @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.tenantId = :tenantId")
  Optional<Usuario> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") String tenantId);

  @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.activo = true")
  Optional<Usuario> findByEmailAndActive(@Param("email") String email);

  @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Usuario u WHERE u.email = :email AND u.tenantId = :tenantId")
  boolean existsByEmailAndTenantId(@Param("email") String email, @Param("tenantId") String tenantId);

  @Modifying
  @Query("UPDATE Usuario u SET u.intentosFallidos = u.intentosFallidos + 1 WHERE u.email = :email")
  void incrementarIntentosFallidos(@Param("email") String email);

  @Modifying
  @Query("UPDATE Usuario u SET u.intentosFallidos = 0 WHERE u.email = :email")
  void resetearIntentosFallidos(@Param("email") String email);

  @Modifying
  @Query("UPDATE Usuario u SET u.cuentaBloqueada = true WHERE u.email = :email")
  void bloquearCuenta(@Param("email") String email);

  List<Usuario> findByTenantId(String tenantId);

  Page<Usuario> findByTenantId(String tenantId, Pageable pageable);

  @Query("SELECT u FROM Usuario u WHERE u.tenantId = :tenantId AND u.activo = true")
  Page<Usuario> findActiveByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

  @Query("SELECT u FROM Usuario u WHERE u.roles = :rolId AND u.tenantId = :tenantId")
  List<Usuario> findByRolIdAndTenantId(@Param("rolId") UUID rolId, @Param("tenantId") String tenantId);

  @Query("SELECT u FROM Usuario u JOIN u.sucursales s WHERE s.id = :sucursalId")
  List<Usuario> findBySucursalId(@Param("sucursalId") UUID sucursalId);

  @Query("SELECT u FROM Usuario u JOIN u.cajas c WHERE c.id = :cajaId")
  List<Usuario> findByCajaId(@Param("cajaId") UUID cajaId);
}