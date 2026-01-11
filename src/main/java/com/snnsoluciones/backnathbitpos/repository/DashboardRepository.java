package com.snnsoluciones.backnathbitpos.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ==================== QUERIES ENDPOINT 1 (Lista Simple) ====================

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

    // ==================== QUERIES ENDPOINT 2 (Dashboard Detallado) ====================

    /**
     * Calcula métricas de ventas (hoy, semana, mes) en una sola query
     * Retorna Object[] con: [0]=ventasHoy, [1]=ventasSemana, [2]=ventasMes
     *
     * @param empresaId ID de la empresa
     * @param fechaHoy Fecha actual
     * @param fechaInicioSemana Fecha de inicio de la semana (hace 7 días)
     * @param fechaInicioMes Fecha de inicio del mes actual
     * @return Object[] con las 3 métricas
     */
    @Query("SELECT " +
        "  COALESCE(SUM(CASE WHEN CAST(f.fechaEmision AS date) = :fechaHoy THEN f.totalComprobante ELSE 0 END), 0), " +
        "  COALESCE(SUM(CASE WHEN CAST(f.fechaEmision AS date) >= :fechaInicioSemana THEN f.totalComprobante ELSE 0 END), 0), " +
        "  COALESCE(SUM(CASE WHEN CAST(f.fechaEmision AS date) >= :fechaInicioMes THEN f.totalComprobante ELSE 0 END), 0) " +
        "FROM Factura f " +
        "WHERE f.sucursal.empresa.id = :empresaId")
    Object[] calcularMetricasVentas(
        @Param("empresaId") Long empresaId,
        @Param("fechaHoy") LocalDate fechaHoy,
        @Param("fechaInicioSemana") LocalDate fechaInicioSemana,
        @Param("fechaInicioMes") LocalDate fechaInicioMes
    );

    /**
     * Cuenta cajas abiertas de una empresa
     *
     * @param empresaId ID de la empresa
     * @return Cantidad de sesiones de caja abiertas
     */
    @Query("SELECT COUNT(sc) " +
        "FROM SesionCaja sc " +
        "WHERE sc.terminal.sucursal.empresa.id = :empresaId " +
        "AND sc.estado = 'ABIERTA'")
    Long contarCajasAbiertas(@Param("empresaId") Long empresaId);

    /**
     * Cuenta terminales/PDVs activos de una empresa
     *
     * @param empresaId ID de la empresa
     * @return Cantidad de terminales activos
     */
    @Query("SELECT COUNT(t) " +
        "FROM Terminal t " +
        "WHERE t.sucursal.empresa.id = :empresaId " +
        "AND t.activa = true")
    Long contarPdvsActivos(@Param("empresaId") Long empresaId);

    /**
     * Cuenta usuarios activos de una empresa
     *
     * @param empresaId ID de la empresa
     * @return Cantidad de usuarios activos
     */
    @Query("SELECT COUNT(DISTINCT u) " +
        "FROM Usuario u " +
        "JOIN UsuarioEmpresa ue ON ue.usuario = u " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND u.activo = true " +
        "AND ue.activo = true")
    Long contarUsuariosActivos(@Param("empresaId") Long empresaId);

    /**
     * Obtiene métricas por sucursal (ventas hoy, cajas abiertas, pdvs activos)
     * Retorna Object[] con: [0]=sucursalId, [1]=nombre, [2]=ventasHoy, [3]=cajasAbiertas, [4]=pdvsActivos
     *
     * @param empresaId ID de la empresa
     * @param fechaHoy Fecha actual
     * @return Lista de métricas por sucursal
     */
    @Query("SELECT " +
        "  s.id, " +
        "  s.nombre, " +
        "  COALESCE(SUM(CASE WHEN CAST(f.fechaEmision AS date) = :fechaHoy THEN f.totalComprobante ELSE 0 END), 0), " +
        "  COUNT(DISTINCT CASE WHEN sc.estado = 'ABIERTA' THEN sc.id ELSE NULL END), " +
        "  COUNT(DISTINCT CASE WHEN t.activa = true THEN t.id ELSE NULL END) " +
        "FROM Sucursal s " +
        "LEFT JOIN Factura f ON f.sucursal.id = s.id " +
        "LEFT JOIN Terminal t ON t.sucursal.id = s.id " +
        "LEFT JOIN SesionCaja sc ON sc.terminal.sucursal.id = s.id AND sc.estado = 'ABIERTA' " +
        "WHERE s.empresa.id = :empresaId " +
        "GROUP BY s.id, s.nombre " +
        "ORDER BY s.nombre")
    List<Object[]> obtenerMetricasSucursales(
        @Param("empresaId") Long empresaId,
        @Param("fechaHoy") LocalDate fechaHoy
    );

    /**
     * Obtiene ventas agrupadas por día (últimos 7 días)
     * Retorna Object[] con: [0]=fecha, [1]=totalVentas
     *
     * @param empresaId ID de la empresa
     * @param fechaInicio Fecha de inicio (hace 7 días)
     * @return Lista de ventas por día
     */
    @Query("SELECT CAST(f.fechaEmision AS date), COALESCE(SUM(f.totalComprobante), 0) " +
        "FROM Factura f " +
        "WHERE f.sucursal.empresa.id = :empresaId " +
        "AND CAST(f.fechaEmision AS date) >= :fechaInicio " +
        "GROUP BY CAST(f.fechaEmision AS date) " +
        "ORDER BY CAST(f.fechaEmision AS date) ASC")
    List<Object[]> obtenerVentasPorDia(
        @Param("empresaId") Long empresaId,
        @Param("fechaInicio") LocalDate fechaInicio
    );

    /**
     * Obtiene top productos más vendidos HOY
     * Retorna Object[] con: [0]=nombreProducto, [1]=cantidadVendida, [2]=montoTotal
     *
     * @param empresaId ID de la empresa
     * @param fechaHoy Fecha actual
     * @param pageable Paginación (usar PageRequest.of(0, 5) para top 5)
     * @return Lista de top productos
     */
    @Query("SELECT " +
        "  p.nombre, " +
        "  SUM(fd.cantidad), " +
        "  SUM(fd.montoTotalLinea) " +
        "FROM FacturaDetalle fd " +
        "JOIN fd.producto p " +
        "JOIN fd.factura f " +
        "WHERE f.sucursal.empresa.id = :empresaId " +
        "AND CAST(f.fechaEmision AS date) = :fechaHoy " +
        "GROUP BY p.id, p.nombre " +
        "ORDER BY SUM(fd.cantidad) DESC")
    List<Object[]> obtenerTopProductosHoy(
        @Param("empresaId") Long empresaId,
        @Param("fechaHoy") LocalDate fechaHoy,
        Pageable pageable
    );

    /**
     * Lista cajas abiertas con detalles
     * Retorna Object[] con: [0]=sesionId, [1]=sucursalNombre, [2]=usuarioNombre,
     *                       [3]=usuarioApellidos, [4]=montoInicial, [5]=horaApertura
     *
     * @param empresaId ID de la empresa
     * @return Lista de cajas abiertas
     */
    @Query("SELECT " +
        "  sc.id, " +
        "  s.nombre, " +
        "  u.nombre, " +
        "  u.apellidos, " +
        "  sc.montoInicial, " +
        "  sc.fechaHoraApertura " +
        "FROM SesionCaja sc " +
        "JOIN sc.terminal t " +
        "JOIN t.sucursal s " +
        "JOIN sc.usuario u " +
        "WHERE s.empresa.id = :empresaId " +
        "AND sc.estado = 'ABIERTA' " +
        "ORDER BY sc.fechaHoraApertura DESC")
    List<Object[]> listarCajasAbiertas(@Param("empresaId") Long empresaId);
}