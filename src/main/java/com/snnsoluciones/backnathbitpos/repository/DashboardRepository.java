package com.snnsoluciones.backnathbitpos.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository custom para queries del Dashboard Administrativo
 * Contiene queries optimizados para métricas y estadísticas
 *
 * NO extiende JpaRepository para evitar operaciones CRUD innecesarias
 * Solo define queries de lectura específicas
 */
@org.springframework.stereotype.Repository
public interface DashboardRepository extends Repository<com.snnsoluciones.backnathbitpos.entity.Factura, Long> {

    /**
     * Calcula el total de ventas de HOY para una empresa específica
     *
     * @param empresaId ID de la empresa
     * @return Total de ventas del día actual (0 si no hay ventas)
     */
    @Query("SELECT COALESCE(SUM(f.totalComprobante), 0) " +
        "FROM Factura f " +
        "WHERE f.sucursal.empresa.id = :empresaId " +
        "AND CAST(f.fechaEmision AS date) = CURRENT_DATE")
    BigDecimal calcularVentasHoyPorEmpresa(@Param("empresaId") Long empresaId);

    /**
     * Calcula ventas de HOY para múltiples empresas en una sola query (bulk)
     * Más eficiente que llamar calcularVentasHoyPorEmpresa() en loop
     *
     * @param empresasIds Lista de IDs de empresas
     * @return Lista de Object[] donde [0]=empresaId (Long), [1]=totalVentas (BigDecimal)
     */
    @Query("SELECT f.sucursal.empresa.id, COALESCE(SUM(f.totalComprobante), 0) " +
        "FROM Factura f " +
        "WHERE f.sucursal.empresa.id IN :empresasIds " +
        "AND CAST(f.fechaEmision AS date) = CURRENT_DATE " +
        "GROUP BY f.sucursal.empresa.id")
    List<Object[]> calcularVentasHoyPorEmpresas(@Param("empresasIds") List<Long> empresasIds);
}