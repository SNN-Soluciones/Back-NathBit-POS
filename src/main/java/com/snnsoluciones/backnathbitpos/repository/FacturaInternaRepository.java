package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInterna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaInternaRepository extends JpaRepository<FacturaInterna, Long> {

    // Buscar por número
    Optional<FacturaInterna> findByNumero(String numero);

    // Buscar por empresa
    Page<FacturaInterna> findByEmpresaId(Long empresaId, Pageable pageable);

    // Buscar por sucursal
    Page<FacturaInterna> findBySucursalId(Long sucursalId, Pageable pageable);

    // Buscar por empresa y sucursal
    Page<FacturaInterna> findByEmpresaIdAndSucursalId(Long empresaId, Long sucursalId, Pageable pageable);

    // Buscar por cajero
    List<FacturaInterna> findByCajeroId(Long cajeroId);

    // Buscar por cliente
    List<FacturaInterna> findByClienteId(Long clienteId);

    // Buscar por estado
    Page<FacturaInterna> findByEmpresaIdAndEstado(Long empresaId, String estado, Pageable pageable);

    // Buscar por rango de fechas
    @Query("SELECT f FROM FacturaInterna f WHERE f.empresa.id = :empresaId " +
        "AND f.fecha BETWEEN :fechaInicio AND :fechaFin " +
        "ORDER BY f.fecha DESC")
    List<FacturaInterna> findByEmpresaAndFechaRange(
        @Param("empresaId") Long empresaId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    // Obtener el último número de factura para generar el siguiente
    @Query("SELECT f.numero FROM FacturaInterna f WHERE f.empresa.id = :empresaId " +
        "AND f.numero LIKE :prefix% " +
        "ORDER BY f.numero DESC")
    List<String> findUltimoNumeroByEmpresaAndPrefix(
        @Param("empresaId") Long empresaId,
        @Param("prefix") String prefix,
        Pageable pageable
    );

    // Contar facturas del día
    @Query("SELECT COUNT(f) FROM FacturaInterna f WHERE f.sucursal.id = :sucursalId " +
        "AND DATE(f.fecha) = CURRENT_DATE")
    Long countFacturasHoyBySucursal(@Param("sucursalId") Long sucursalId);

    // Total vendido del día por sucursal
    @Query("SELECT SUM(f.total) FROM FacturaInterna f WHERE f.sucursal.id = :sucursalId " +
        "AND DATE(f.fecha) = CURRENT_DATE AND f.estado = 'PAGADA'")
    Optional<java.math.BigDecimal> sumTotalHoyBySucursal(@Param("sucursalId") Long sucursalId);

    // Buscar facturas para reporte diario
    @Query("SELECT f FROM FacturaInterna f " +
        "WHERE f.sucursal.id = :sucursalId " +
        "AND DATE(f.fecha) = :fecha " +
        "ORDER BY f.fecha ASC")
    List<FacturaInterna> findBySucursalAndFecha(
        @Param("sucursalId") Long sucursalId,
        @Param("fecha") java.time.LocalDate fecha
    );
}