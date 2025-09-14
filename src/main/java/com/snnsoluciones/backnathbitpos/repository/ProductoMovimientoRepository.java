package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoMovimiento;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductoMovimientoRepository extends JpaRepository<ProductoMovimiento, Long> {
    
    /**
     * Buscar movimientos por producto
     */
    List<ProductoMovimiento> findByProductoIdOrderByFechaMovimientoDesc(Long productoId);
    
    /**
     * Buscar movimientos por producto con paginación
     */
    Page<ProductoMovimiento> findByProductoId(Long productoId, Pageable pageable);
    
    /**
     * Buscar movimientos por sucursal
     */
    List<ProductoMovimiento> findBySucursalIdOrderByFechaMovimientoDesc(Long sucursalId);
    
    /**
     * Buscar movimientos por producto y sucursal
     */
    List<ProductoMovimiento> findByProductoIdAndSucursalIdOrderByFechaMovimientoDesc(
        Long productoId, Long sucursalId
    );
    
    /**
     * Buscar movimientos por tipo
     */
    List<ProductoMovimiento> findByTipoMovimientoOrderByFechaMovimientoDesc(
        TipoMovimiento tipoMovimiento
    );
    
    /**
     * Buscar movimientos por documento de referencia
     */
    List<ProductoMovimiento> findByDocumentoReferencia(String documentoReferencia);
    
    /**
     * Buscar movimientos por rango de fechas
     */
    @Query("SELECT m FROM ProductoMovimiento m " +
           "WHERE m.fechaMovimiento BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY m.fechaMovimiento DESC")
    List<ProductoMovimiento> findByFechaMovimientoBetween(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * Buscar movimientos por producto y rango de fechas
     */
    @Query("SELECT m FROM ProductoMovimiento m " +
           "WHERE m.producto.id = :productoId " +
           "AND m.fechaMovimiento BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY m.fechaMovimiento DESC")
    List<ProductoMovimiento> findByProductoAndFechaBetween(
        @Param("productoId") Long productoId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * Obtener el último movimiento de un producto en una sucursal
     */
    @Query("SELECT m FROM ProductoMovimiento m " +
           "WHERE m.producto.id = :productoId " +
           "AND m.sucursal.id = :sucursalId " +
           "ORDER BY m.fechaMovimiento DESC, m.id DESC " +
           "LIMIT 1")
    ProductoMovimiento findUltimoMovimiento(
        @Param("productoId") Long productoId,
        @Param("sucursalId") Long sucursalId
    );
    
    /**
     * Resumen de movimientos por tipo en un período
     */
    @Query("SELECT m.tipoMovimiento, COUNT(m), SUM(m.cantidad), SUM(m.costoTotal) " +
           "FROM ProductoMovimiento m " +
           "WHERE m.sucursal.id = :sucursalId " +
           "AND m.fechaMovimiento BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY m.tipoMovimiento")
    List<Object[]> resumenMovimientosPorTipo(
        @Param("sucursalId") Long sucursalId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
}