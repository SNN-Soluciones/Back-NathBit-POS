package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimientoCaja;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

  @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCaja m " +
      "WHERE m.sesionCaja.id = :sesionId " +
      "AND m.tipoMovimiento = :tipo")
  BigDecimal sumBySesionIdAndTipo(@Param("sesionId") Long sesionId,
      @Param("tipo") TipoMovimientoCaja tipo);

  // Obtener todos los movimientos de una sesión
  List<MovimientoCaja> findBySesionCajaIdOrderByFechaHoraDesc(Long sesionId);

  // Sumar todos los vales (salidas) de una sesión
  @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCaja m " +
      "WHERE m.sesionCaja.id = :sesionId " +
      "AND m.tipoMovimiento IN ('SALIDA_VALE', 'SALIDA_DEPOSITO')")
  BigDecimal sumSalidasBySesionId(@Param("sesionId") Long sesionId);

  // Sumar todas las entradas adicionales de una sesión
  @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCaja m " +
      "WHERE m.sesionCaja.id = :sesionId " +
      "AND m.tipoMovimiento = 'ENTRADA_ADICIONAL'")
  BigDecimal sumEntradasBySesionId(@Param("sesionId") Long sesionId);

  // Contar movimientos por tipo en una sesión
  @Query("SELECT COUNT(m) FROM MovimientoCaja m " +
      "WHERE m.sesionCaja.id = :sesionId " +
      "AND m.tipoMovimiento = :tipo")
  Long countBySesionIdAndTipo(@Param("sesionId") Long sesionId,
      @Param("tipo") TipoMovimientoCaja tipo);

  // Obtener resumen de movimientos agrupados por tipo
  @Query("SELECT m.tipoMovimiento, COUNT(m), COALESCE(SUM(m.monto), 0) " +
      "FROM MovimientoCaja m " +
      "WHERE m.sesionCaja.id = :sesionId " +
      "GROUP BY m.tipoMovimiento")
  List<Object[]> getResumenMovimientosPorSesion(@Param("sesionId") Long sesionId);

}
