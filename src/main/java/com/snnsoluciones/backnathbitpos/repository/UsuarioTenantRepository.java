package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.security.UsuarioTenant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioTenantRepository extends JpaRepository<UsuarioTenant, UUID> {
    
    List<UsuarioTenant> findByUsuarioIdAndActivo(UUID usuarioId, boolean activo);
    
    Optional<UsuarioTenant> findByUsuarioEmailAndTenantIdAndActivo(
        String email, String tenantId, boolean activo);
    
    Optional<UsuarioTenant> findByUsuarioIdAndTenantIdAndActivo(
        UUID usuarioId, String tenantId, boolean activo);
    
    @Query("SELECT ut FROM UsuarioTenant ut WHERE ut.usuario.email = :email AND ut.activo = true")
    List<UsuarioTenant> findActiveByUsuarioEmail(@Param("email") String email);
}