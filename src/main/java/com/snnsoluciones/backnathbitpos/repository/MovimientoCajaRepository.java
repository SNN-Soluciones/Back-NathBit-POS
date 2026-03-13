package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimientoCaja;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

  List<MovimientoCaja> findBySesionCajaUsuarioId(Long sesionCajaUsuarioId);

  @Query("""
    SELECT COALESCE(SUM(m.monto), 0)
    FROM MovimientoCaja m
    WHERE m.sesionCaja.id = :sesionId
    AND m.tipoMovimiento IN ('SALIDA_VALE', 'SALIDA_DEPOSITO')
    AND m.fechaHora <= :hasta
    """)
  BigDecimal sumSalidasBySesionIdHasta(
      @Param("sesionId") Long sesionId,
      @Param("hasta") LocalDateTime hasta);

  @Query("""
      SELECT COALESCE(SUM(m.monto),0)
      FROM MovimientoCaja m
      WHERE m.sesionCajaUsuario.id = :sesionUsuarioId
      AND m.tipoMovimiento = :tipo
      """)
  BigDecimal sumBySesionUsuarioAndTipo(
      @Param("sesionUsuarioId") Long sesionUsuarioId,
      @Param("tipo") TipoMovimientoCaja tipo);

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

  List<MovimientoCaja> findBySesionCajaIdAndTipoMovimiento(Long sesionCajaId,
      TipoMovimientoCaja tipo);

  List<MovimientoCaja> findBySesionCajaIdOrderByFechaHoraAsc(Long sesionId);

}
