package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.factura.BuscarFacturaReferenciaRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.CrearFacturaRequest;

import com.snnsoluciones.backnathbitpos.dto.factura.FacturaReferenciaDto;
import com.snnsoluciones.backnathbitpos.dto.factura.ValidacionTotalesRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.ValidacionTotalesResponse;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * Servicio para gestión de facturas
 */
public interface FacturaService {

    /**
     * Crear nueva factura con todos sus componentes
     */
    Factura crear(CrearFacturaRequest request);

    /**
     * Buscar factura por ID
     */
    Optional<Factura> buscarPorId(Long id);

    /**
     * Buscar factura por clave
     */
    Optional<Factura> buscarPorClave(String clave);

    /**
     * Buscar factura por consecutivo
     */
    Optional<Factura> buscarPorConsecutivo(String consecutivo);

    /**
     * Listar facturas por sesión de caja
     */
    List<Factura> listarPorSesionCaja(Long sesionCajaId);

    /**
     * Listar facturas con error de una sucursal
     */
    List<Factura> listarFacturasConError(Long sucursalId);

    /**
     * Anular factura
     */
    Factura anular(Long facturaId, String motivo);

    /**
     * Reenviar factura a Hacienda
     */
    void reenviar(Long facturaId);

    /**
     * Validar totales antes de crear la factura
     */
    ValidacionTotalesResponse validarTotales(ValidacionTotalesRequest request);

    /**
     * Buscar facturas para ser usadas como referencia
     */
    Page<FacturaReferenciaDto> buscarParaReferencia(BuscarFacturaReferenciaRequest request);
}