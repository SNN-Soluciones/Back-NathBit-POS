package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumentoExoneracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteExoneracionRepository extends JpaRepository<ClienteExoneracion, Long> {

    @Modifying
    @Query("DELETE FROM ClienteExoneracion e WHERE e.cliente.id = :clienteId")
    void deleteAllByClienteId(@Param("clienteId") Long clienteId);

    @Modifying
    @Query(value = "UPDATE clientes_exoneraciones SET cliente_id = :clienteId WHERE id = :exoneracionId", nativeQuery = true)
    void updateClienteId(@Param("exoneracionId") Long exoneracionId, @Param("clienteId") Long clienteId);
    
    // Buscar exoneraciones activas por cliente
    List<ClienteExoneracion> findByClienteIdAndActivoTrue(Long clienteId);
    
    // Buscar exoneración vigente por cliente
    @Query("SELECT e FROM ClienteExoneracion e " +
           "WHERE e.cliente.id = :clienteId " +
           "AND e.activo = true " +
           "AND (e.fechaVencimiento IS NULL OR e.fechaVencimiento >= :fecha)")
    List<ClienteExoneracion> findExoneracionesVigentes(
        @Param("clienteId") Long clienteId,
        @Param("fecha") LocalDate fecha
    );
    
    boolean existsByNumeroDocumentoAndClienteIdNotAndActivoTrue(
        String numeroDocumento, 
        Long clienteId
    );
    
    // Contar exoneraciones activas por sucursal
    @Query("SELECT COUNT(e) FROM ClienteExoneracion e " +
           "WHERE e.cliente.empresa.id = :sucursalId " +
           "AND e.activo = true")
    long countExoneracionesActivasPorSucursal(@Param("sucursalId") Long sucursalId);
    
    // Desactivar exoneraciones vencidas
    @Modifying
    @Query("UPDATE ClienteExoneracion e " +
           "SET e.activo = false " +
           "WHERE e.activo = true " +
           "AND e.fechaVencimiento < :fecha")
    int desactivarExoneracionesVencidas(@Param("fecha") LocalDate fecha);
}