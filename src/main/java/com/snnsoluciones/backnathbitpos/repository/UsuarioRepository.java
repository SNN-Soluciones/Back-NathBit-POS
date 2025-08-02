package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
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
}