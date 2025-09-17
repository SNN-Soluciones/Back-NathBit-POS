package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Pago;
import com.snnsoluciones.backnathbitpos.enums.EstadoPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    
    // Pagos de una cuenta
    List<Pago> findByCuentaPorCobrarIdAndEstadoOrderByFechaPagoDesc(
        Long cuentaId, 
        EstadoPago estado
    );
    
    // Pagos por sesión de caja
    List<Pago> findBySesionCajaIdAndEstado(Long sesionId, EstadoPago estado);
    
    // Último número de recibo
    @Query("SELECT p.numeroRecibo FROM Pago p WHERE p.sesionCaja.terminal.sucursal.id = :sucursalId " +
           "ORDER BY p.id DESC LIMIT 1")
    String findUltimoNumeroRecibo(@Param("sucursalId") Long sucursalId);
    
    // Total cobrado en un período
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
           "WHERE p.cliente.empresa.id = :empresaId " +
           "AND p.fechaPago BETWEEN :inicio AND :fin " +
           "AND p.estado = 'APLICADO'")
    BigDecimal sumTotalCobrado(
        @Param("empresaId") Long empresaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    /**
     * Alternativa con Order by para ordenar por fecha
     */
    List<Pago> findByFechaPagoBetweenAndEstadoOrderByFechaPagoDesc(
        LocalDateTime inicio,
        LocalDateTime fin,
        EstadoPago estado
    );
}