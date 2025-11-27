// src/main/java/com/snnsoluciones/backnathbitpos/repository/HistorialPagoRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.HistorialPago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HistorialPagoRepository extends JpaRepository<HistorialPago, Long> {
    
    /**
     * Obtener historial de pagos de una sucursal
     */
    List<HistorialPago> findBySucursalIdOrderByFechaPagoDesc(Long sucursalId);
    
    /**
     * Obtener historial de pagos de una empresa
     */
    List<HistorialPago> findByEmpresaIdOrderByFechaPagoDesc(Long empresaId);
    
    /**
     * Buscar pagos de una sucursal en un período
     */
    @Query("SELECT h FROM HistorialPago h WHERE h.sucursal.id = :sucursalId " +
           "AND h.fechaPago BETWEEN :inicio AND :fin ORDER BY h.fechaPago DESC")
    List<HistorialPago> findBySucursalAndPeriodo(
        @Param("sucursalId") Long sucursalId,
        @Param("inicio") LocalDate inicio,
        @Param("fin") LocalDate fin
    );
    
    /**
     * Buscar pagos de una empresa en un período
     */
    @Query("SELECT h FROM HistorialPago h WHERE h.empresa.id = :empresaId " +
           "AND h.fechaPago BETWEEN :inicio AND :fin ORDER BY h.fechaPago DESC")
    List<HistorialPago> findByEmpresaAndPeriodo(
        @Param("empresaId") Long empresaId,
        @Param("inicio") LocalDate inicio,
        @Param("fin") LocalDate fin
    );
    
    /**
     * Obtener último pago de una sucursal
     */
    @Query("SELECT h FROM HistorialPago h WHERE h.sucursal.id = :sucursalId " +
           "ORDER BY h.fechaPago DESC, h.createdAt DESC LIMIT 1")
    Optional<HistorialPago> findUltimoPagoBySucursal(@Param("sucursalId") Long sucursalId);
    
    /**
     * Sumar total pagado por una sucursal
     */
    @Query("SELECT COALESCE(SUM(h.monto), 0) FROM HistorialPago h WHERE h.sucursal.id = :sucursalId")
    BigDecimal sumTotalPagadoBySucursal(@Param("sucursalId") Long sucursalId);
    
    /**
     * Sumar total pagado por una empresa
     */
    @Query("SELECT COALESCE(SUM(h.monto), 0) FROM HistorialPago h WHERE h.empresa.id = :empresaId")
    BigDecimal sumTotalPagadoByEmpresa(@Param("empresaId") Long empresaId);
    
    /**
     * Contar pagos de una sucursal
     */
    Long countBySucursalId(Long sucursalId);
    
    /**
     * Verificar si una sucursal tiene pagos registrados
     */
    boolean existsBySucursalId(Long sucursalId);
}