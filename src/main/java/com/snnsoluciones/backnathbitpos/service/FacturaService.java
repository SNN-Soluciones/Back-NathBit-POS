package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaDetalle;
import com.snnsoluciones.backnathbitpos.entity.FacturaMedioPago;

import java.util.List;
import java.util.Optional;

/**
 * Servicio principal para gestión de facturas
 */
public interface FacturaService {
    
    /**
     * Crea una nueva factura con generación inmediata de consecutivo y clave
     * 
     * @param factura Datos de la factura
     * @param detalles Lista de productos
     * @param mediosPago Lista de medios de pago
     * @return Factura creada con clave y consecutivo
     */
    Factura crear(Factura factura, List<FacturaDetalle> detalles, List<FacturaMedioPago> mediosPago);
    
    /**
     * Busca una factura por su clave única
     */
    Optional<Factura> buscarPorClave(String clave);
    
    /**
     * Busca una factura por su consecutivo
     */
    Optional<Factura> buscarPorConsecutivo(String consecutivo);
    
    /**
     * Lista las facturas de una sesión de caja
     */
    List<Factura> listarPorSesionCaja(Long sesionCajaId);
    
    /**
     * Lista facturas con error de una sucursal
     */
    List<Factura> listarFacturasConError(Long sucursalId);
    
    /**
     * Anula una factura (si el estado lo permite)
     */
    Factura anular(Long facturaId, String motivo);
    
    /**
     * Reenvía una factura rechazada o con error
     */
    void reenviar(Long facturaId);
}