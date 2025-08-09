package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.TipoCambio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TipoCambioService {
    
    // Gestión de tipos de cambio
    TipoCambio crear(TipoCambio tipoCambio);
    TipoCambio actualizar(Long id, TipoCambio tipoCambio);
    Optional<TipoCambio> buscarPorId(Long id);
    
    // Consultas
    Optional<TipoCambio> buscarPorMonedaYFecha(String codigoMoneda, LocalDate fecha);
    TipoCambio obtenerTipoCambioActual(String codigoMoneda);
    List<TipoCambio> listarPorFechaRango(LocalDate fechaInicio, LocalDate fechaFin);
    List<TipoCambio> listarUltimosPorMoneda(String codigoMoneda, int cantidad);
    
    // Conversiones
    BigDecimal convertir(BigDecimal monto, String monedaOrigen, String monedaDestino, LocalDate fecha);
    BigDecimal convertirAColones(BigDecimal monto, String monedaOrigen);
    BigDecimal convertirDeColones(BigDecimal monto, String monedaDestino);
    
    // Actualización masiva (para API BCCR)
    void actualizarTiposCambioDiarios();
}