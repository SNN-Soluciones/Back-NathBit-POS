package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumentoExoneracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteExoneracionRepository extends JpaRepository<ClienteExoneracion, Long> {
    
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
    
    // Buscar por número de documento
    Optional<ClienteExoneracion> findByNumeroDocumentoAndActivoTrue(String numeroDocumento);
    
    // Verificar si existe exoneración con mismo número
    boolean existsByNumeroDocumentoAndClienteIdNotAndActivoTrue(
        String numeroDocumento, 
        Long clienteId
    );
    
    // Buscar exoneraciones próximas a vencer
    @Query("SELECT e FROM ClienteExoneracion e " +
           "WHERE e.cliente.empresa.id = :sucursalId " +
           "AND e.activo = true " +
           "AND e.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY e.fechaVencimiento ASC")
    List<ClienteExoneracion> findExoneracionesProximasAVencer(
        @Param("sucursalId") Long sucursalId,
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
    );
    
    // Buscar por tipo de documento
    List<ClienteExoneracion> findByClienteIdAndTipoDocumentoAndActivoTrue(
        Long clienteId,
        TipoDocumentoExoneracion tipoDocumento
    );

    
    // Contar exoneraciones activas por sucursal
    @Query("SELECT COUNT(e) FROM ClienteExoneracion e " +
           "WHERE e.cliente.empresa.id = :sucursalId " +
           "AND e.activo = true")
    long countExoneracionesActivasPorSucursal(@Param("sucursalId") Long sucursalId);
    
    // Desactivar exoneraciones vencidas
    @Query("UPDATE ClienteExoneracion e " +
           "SET e.activo = false " +
           "WHERE e.activo = true " +
           "AND e.fechaVencimiento < :fecha")
    int desactivarExoneracionesVencidas(@Param("fecha") LocalDate fecha);
}