package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.movimiento.HistorialMovimientosResponse;
import com.snnsoluciones.backnathbitpos.dto.movimiento.MovimientoCajaDTO;
import com.snnsoluciones.backnathbitpos.dto.movimiento.RegistrarEntradaRequest;
import com.snnsoluciones.backnathbitpos.dto.movimiento.RegistrarSalidaRequest;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import java.math.BigDecimal;
import java.util.List;

public interface MovimientoCajaService {

  /**
   * Registra un vale (salida de efectivo)
   */
  MovimientoCaja registrarVale(Long sesionId, BigDecimal monto, String concepto);

  /**
   * Obtiene todos los movimientos de una sesión
   */
  List<MovimientoCaja> obtenerMovimientosPorSesion(Long sesionId);

  /**
   * Obtiene el total de vales de una sesión
   */
  BigDecimal obtenerTotalVales(Long sesionId);

  /**
   * 🆕 Registra una salida de efectivo (Arqueo, Pago Proveedor, Otros)
   *
   * @param sesionId ID de la sesión de caja
   * @param request Datos de la salida
   * @return DTO con información del movimiento creado
   */
  MovimientoCajaDTO registrarSalida(Long sesionId, RegistrarSalidaRequest request);

  /**
   * 🆕 Registra una entrada de efectivo
   *
   * @param sesionId ID de la sesión de caja
   * @param request Datos de la entrada
   * @return DTO con información del movimiento creado
   */
  MovimientoCajaDTO registrarEntrada(Long sesionId, RegistrarEntradaRequest request);

  /**
   * 🆕 Obtiene el historial completo con totales segregados
   *
   * @param sesionId ID de la sesión
   * @return Historial con movimientos y totales por tipo
   */
  HistorialMovimientosResponse obtenerHistorialCompleto(Long sesionId);

  /**
   * 🆕 Obtiene el total de salidas por tipo específico
   *
   * @param sesionId ID de la sesión
   * @param tipoMovimiento Tipo de movimiento (ej: "SALIDA_ARQUEO")
   * @return Total del tipo especificado
   */
  BigDecimal obtenerTotalSalidasPorTipo(Long sesionId, String tipoMovimiento);

}