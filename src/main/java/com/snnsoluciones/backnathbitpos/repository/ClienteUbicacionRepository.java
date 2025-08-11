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
    
    // Verificar si cliente tiene ubicación
    boolean existsByClienteId(Long clienteId);
    
    // Buscar clientes por ubicación geográfica
    @Query("SELECT cu FROM ClienteUbicacion cu " +
           "WHERE cu.cliente.sucursal.id = :sucursalId " +
           "AND cu.provincia.id = :provinciaId " +
           "AND (:cantonId IS NULL OR cu.canton.id = :cantonId) " +
           "AND (:distritoId IS NULL OR cu.distrito.id = :distritoId)")
    List<ClienteUbicacion> findByUbicacionGeografica(
        @Param("sucursalId") Long sucursalId,
        @Param("provinciaId") String provinciaId,
        @Param("cantonId") String cantonId,
        @Param("distritoId") String distritoId
    );
    
    // Contar clientes con ubicación por sucursal
    @Query("SELECT COUNT(cu) FROM ClienteUbicacion cu " +
           "WHERE cu.cliente.sucursal.id = :sucursalId " +
           "AND cu.cliente.activo = true")
    long countClientesConUbicacionPorSucursal(@Param("sucursalId") Long sucursalId);
}