package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteUbicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteUbicacionRepository extends JpaRepository<ClienteUbicacion, Long> {
    
    // Buscar ubicación por cliente
    Optional<ClienteUbicacion> findByClienteId(Long clienteId);
}