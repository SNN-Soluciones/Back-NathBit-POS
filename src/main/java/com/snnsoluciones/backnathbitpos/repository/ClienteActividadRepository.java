package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteActividadRepository extends JpaRepository<ClienteActividad, Long> {
    
    List<ClienteActividad> findByClienteId(Long clienteId);
    
    void deleteByClienteId(Long clienteId);
    
    @Query("SELECT ca FROM ClienteActividad ca WHERE ca.cliente.id = :clienteId ORDER BY ca.codigoActividad")
    List<ClienteActividad> findByClienteIdOrderByCodigo(@Param("clienteId") Long clienteId);
    
    boolean existsByClienteIdAndCodigoActividad(Long clienteId, String codigoActividad);
}