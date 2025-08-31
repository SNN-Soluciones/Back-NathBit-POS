// src/main/java/com/snnsoluciones/backnathbitpos/repository/ClienteEmailRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteEmailRepository extends JpaRepository<ClienteEmail, Long> {
    
    // Buscar todos los emails de un cliente
    List<ClienteEmail> findByClienteIdOrderByUltimoUsoDesc(Long clienteId);
    
    // Buscar email específico de un cliente
    Optional<ClienteEmail> findByClienteIdAndEmail(Long clienteId, String email);
    
    // Verificar si un email ya existe para cualquier cliente
    boolean existsByEmail(String email);
    
    // Buscar el email más usado de un cliente
    @Query("SELECT ce FROM ClienteEmail ce WHERE ce.cliente.id = :clienteId " +
           "ORDER BY ce.vecesUsado DESC, ce.ultimoUso DESC")
    List<ClienteEmail> findByClienteIdOrderByFrecuenciaUso(@Param("clienteId") Long clienteId);
    
    // Contar emails de un cliente
    long countByClienteId(Long clienteId);
}