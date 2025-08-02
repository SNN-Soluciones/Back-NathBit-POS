package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.security.Rol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolRepository extends JpaRepository<Rol, UUID> {
    
    Optional<Rol> findByNombre(RolNombre nombre);
    
    Boolean existsByNombre(RolNombre nombre);
}