package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import java.math.BigDecimal;
import java.util.List;

public interface MovimientoCajaService {

  /**
   * Registra un vale (salida de efectivo)
   */
  MovimientoCaja registrarVale(Long sesionId, BigDecimal monto, String concepto);

  /**
   * Registra una entrada adicional de efectivo
   */
  MovimientoCaja registrarEntradaAdicional(Long sesionId, BigDecimal monto, String concepto);

  /**
   * Registra un depósito bancario
   */
  MovimientoCaja registrarDeposito(Long sesionId, BigDecimal monto, String concepto);

  /**
   * Obtiene todos los movimientos de una sesión
   */
  List<MovimientoCaja> obtenerMovimientosPorSesion(Long sesionId);

  /**
   * Obtiene el total de vales de una sesión
   */
  BigDecimal obtenerTotalVales(Long sesionId);

  /**
   * Obtiene el total de todas las salidas (vales + depósitos)
   */
  BigDecimal obtenerTotalSalidas(Long sesionId);

  /**
   * Obtiene el total de entradas adicionales
   */
  BigDecimal obtenerTotalEntradas(Long sesionId);

  /**
   * Anula un movimiento creando un movimiento inverso
   */
  MovimientoCaja anularMovimiento(Long movimientoId, String motivo);
}