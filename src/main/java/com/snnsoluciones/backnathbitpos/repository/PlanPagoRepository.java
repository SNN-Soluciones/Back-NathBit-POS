// src/main/java/com/snnsoluciones/backnathbitpos/repository/PlanPagoRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.PlanPago;
import com.snnsoluciones.backnathbitpos.enums.EstadoPlan;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanPagoRepository extends JpaRepository<PlanPago, Long> {
    
    /**
     * Buscar plan por sucursal ID
     */
    Optional<PlanPago> findBySucursalId(Long sucursalId);
    
    /**
     * Buscar todos los planes de una empresa
     */
    List<PlanPago> findByEmpresaId(Long empresaId);
    
    /**
     * Buscar planes por estado
     */
    List<PlanPago> findByEstado(EstadoPlan estado);
    
    /**
     * Buscar planes de una empresa con estado específico
     */
    @Query("SELECT p FROM PlanPago p WHERE p.empresa.id = :empresaId AND p.estado = :estado")
    List<PlanPago> findByEmpresaIdAndEstado(
        @Param("empresaId") Long empresaId, 
        @Param("estado") EstadoPlan estado
    );
    
    /**
     * Buscar planes vencidos o próximos a vencer
     */
    @Query("SELECT p FROM PlanPago p WHERE p.fechaProximoVencimiento <= :fecha AND p.estado = 'ACTIVO'")
    List<PlanPago> findVencidosOProximosAVencer(@Param("fecha") LocalDate fecha);
    
    /**
     * Contar sucursales activas de una empresa
     */
    @Query("SELECT COUNT(p) FROM PlanPago p WHERE p.empresa.id = :empresaId AND p.estado = 'ACTIVO'")
    Long countSucursalesActivasByEmpresa(@Param("empresaId") Long empresaId);
    
    /**
     * Contar sucursales suspendidas de una empresa
     */
    @Query("SELECT COUNT(p) FROM PlanPago p WHERE p.empresa.id = :empresaId AND p.estado = 'SUSPENDIDO'")
    Long countSucursalesSuspendidasByEmpresa(@Param("empresaId") Long empresaId);
    
    /**
     * Obtener suma de cuotas mensuales activas por empresa
     */
    @Query("SELECT COALESCE(SUM(p.cuotaMensual), 0) FROM PlanPago p " +
           "WHERE p.empresa.id = :empresaId AND p.estado = 'ACTIVO'")
    BigDecimal sumCuotasMensualesActivasByEmpresa(@Param("empresaId") Long empresaId);
    
    /**
     * Verificar si existe un plan para una sucursal
     */
    boolean existsBySucursalId(Long sucursalId);

    Long countByEmpresaId(Long empresaId);
}