package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    
    // Buscar por clave de Hacienda
    Optional<Compra> findByClaveHacienda(String claveHacienda);
    
    // Verificar si existe por clave
    boolean existsByClaveHacienda(String claveHacienda);
    
    // Buscar por empresa
    List<Compra> findByEmpresaIdOrderByFechaEmisionDesc(Long empresaId);
    
    // Buscar por sucursal
    List<Compra> findBySucursalIdOrderByFechaEmisionDesc(Long sucursalId);
    
    // Buscar por proveedor
    List<Compra> findByProveedorIdOrderByFechaEmisionDesc(Long proveedorId);
    
    // Buscar por rango de fechas
    @Query("SELECT c FROM Compra c WHERE c.empresa.id = :empresaId " +
           "AND c.fechaEmision BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY c.fechaEmision DESC")
    List<Compra> findByEmpresaAndFechaEmisionBetween(
        @Param("empresaId") Long empresaId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    // Buscar pendientes de envío a Hacienda
    @Query("SELECT c FROM Compra c WHERE c.empresa.id = :empresaId " +
           "AND c.estado = 'PENDIENTE_ENVIO' " +
           "ORDER BY c.createdAt")
    List<Compra> findPendientesEnvio(@Param("empresaId") Long empresaId);
    
    // Buscar por número de documento
    @Query("SELECT c FROM Compra c WHERE c.empresa.id = :empresaId " +
           "AND c.numeroDocumento = :numeroDocumento")
    Optional<Compra> findByEmpresaAndNumeroDocumento(
        @Param("empresaId") Long empresaId,
        @Param("numeroDocumento") String numeroDocumento
    );
    
    // Estadísticas de compras
    @Query("SELECT COUNT(c), SUM(c.totalComprobante) FROM Compra c " +
           "WHERE c.empresa.id = :empresaId " +
           "AND c.fechaEmision BETWEEN :fechaInicio AND :fechaFin " +
           "AND c.estado != 'ANULADA'")
    Object[] obtenerEstadisticasPorPeriodo(
        @Param("empresaId") Long empresaId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
}