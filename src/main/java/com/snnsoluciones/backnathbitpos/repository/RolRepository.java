package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.security.Rol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolRepository extends JpaRepository<Rol, UUID> {
    
    Optional<Rol> findByNombre(RolNombre nombre);
    
    Boolean existsByNombre(RolNombre nombre);

    List<Rol> findByTenantId(String tenantId);

    @Query("SELECT r FROM Rol r JOIN FETCH r.permisos WHERE r.nombre = ?1")
    Optional<Rol> findByNombreWithPermisos(RolNombre nombre);
}