package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInterna;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FacturaInternaRepository extends JpaRepository<FacturaInterna, Long>,
    JpaSpecificationExecutor<FacturaInterna> {
    
    // Buscar por número de factura
    Optional<FacturaInterna> findByNumeroFactura(String numeroFactura);
    
    // Buscar por empresa y sucursal
    Page<FacturaInterna> findByEmpresaIdAndSucursalId(Long empresaId, Long sucursalId, Pageable pageable);
    
    // Buscar facturas de hoy
    @Query(value = "SELECT * FROM factura_interna f WHERE f.sucursal_id = :sucursalId " +
        "AND DATE(f.fecha_emision) = CURRENT_DATE",
        nativeQuery = true)
    List<FacturaInterna> findFacturasHoy(@Param("sucursalId") Long sucursalId);
    
    // Obtener siguiente número de factura
    @Query("SELECT MAX(CAST(SUBSTRING(f.numeroFactura, 9) AS integer)) " +
           "FROM FacturaInterna f WHERE f.sucursal.id = :sucursalId " +
           "AND f.numeroFactura LIKE CONCAT('FI-', :year, '-%')")
    Optional<Integer> findMaxNumeroFactura(@Param("sucursalId") Long sucursalId, 
                                          @Param("year") String year);
    
    // Buscar por rango de fechas
    Page<FacturaInterna> findByFechaEmisionBetween(LocalDateTime inicio, 
                                                   LocalDateTime fin, 
                                                   Pageable pageable);

    // En FacturaInternaRepository.java

    @Query("SELECT f FROM FacturaInterna f WHERE " +
        "(:empresaId IS NULL OR f.empresa.id = :empresaId) AND " +
        "(:sucursalId IS NULL OR f.sucursal.id = :sucursalId) AND " +
        "(:estado IS NULL OR f.estado = :estado) AND " +
        "(:numeroFactura IS NULL OR f.numeroFactura LIKE %:numeroFactura%) AND " +
        "(:fechaInicio IS NULL OR f.fechaEmision >= :fechaInicio) AND " +
        "(:fechaFin IS NULL OR f.fechaEmision <= :fechaFin)")
    Page<FacturaInterna> buscarConFiltros(@Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("estado") String estado,
        @Param("numeroFactura") String numeroFactura,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        Pageable pageable);
}