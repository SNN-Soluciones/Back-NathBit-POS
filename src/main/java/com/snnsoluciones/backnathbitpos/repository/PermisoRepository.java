package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.security.Permiso;
import com.snnsoluciones.backnathbitpos.enums.PermisoNombre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, UUID> {
    Optional<Permiso> findByCodigo(String codigo);
    Boolean existsByCodigo(String codigo);
}