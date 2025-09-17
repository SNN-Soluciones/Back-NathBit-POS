package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.CuentaPorCobrar;
import com.snnsoluciones.backnathbitpos.enums.EstadoCuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {
    
    // Buscar por factura
    Optional<CuentaPorCobrar> findByFacturaId(Long facturaId);
    
    // Por cliente y estado
    Page<CuentaPorCobrar> findByClienteIdAndEstado(
        Long clienteId, 
        EstadoCuenta estado, 
        Pageable pageable
    );
    
    // Todas las cuentas de un cliente
    List<CuentaPorCobrar> findByClienteIdOrderByFechaVencimientoAsc(Long clienteId);
    
    // Cuentas vencidas de una empresa
    @Query("SELECT c FROM CuentaPorCobrar c WHERE c.empresa.id = :empresaId " +
           "AND c.fechaVencimiento < :fecha AND c.estado IN ('VIGENTE', 'PARCIAL')")
    List<CuentaPorCobrar> findVencidas(
        @Param("empresaId") Long empresaId, 
        @Param("fecha") LocalDate fecha
    );
    
    // Suma de saldo por cliente
    @Query("SELECT COALESCE(SUM(c.saldo), 0) FROM CuentaPorCobrar c " +
           "WHERE c.cliente.id = :clienteId AND c.estado IN ('VIGENTE', 'PARCIAL', 'VENCIDA')")
    BigDecimal sumSaldoByClienteId(@Param("clienteId") Long clienteId);

    // En CuentaPorCobrarRepository.java, modificar el query:

    @Query("SELECT c FROM CuentaPorCobrar c WHERE " +
        "c.fechaVencimiento < :fecha AND c.estado IN ('VIGENTE', 'PARCIAL')")
    List<CuentaPorCobrar> findVencidas(@Param("fecha") LocalDate fecha);
}