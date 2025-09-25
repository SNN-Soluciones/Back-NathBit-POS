package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO simplificado para búsqueda de facturas para referencias
 * Solo incluye los campos necesarios para crear referencias
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaReferenciaDto {
    
    /**
     * ID interno de la factura
     */
    private Long id;
    
    /**
     * Clave numérica de 50 dígitos
     */
    private String clave;
    
    /**
     * Consecutivo de la factura (ej: 00100001010000000001)
     */
    private String consecutivo;
    
    /**
     * Fecha de emisión
     */
    private LocalDateTime fechaEmision;
    
    /**
     * Tipo de documento (siempre será "01" para Factura Electrónica)
     */
    private String tipoDocumento;
    
    /**
     * Nombre del cliente
     */
    private String clienteNombre;
    
    /**
     * Identificación del cliente
     */
    private String clienteIdentificacion;
    
    /**
     * Total de la factura
     */
    private BigDecimal totalComprobante;
    
    /**
     * Moneda
     */
    private String moneda;
    
    /**
     * Estado de la factura
     */
    private String estado;
    
    /**
     * Empresa
     */
    private String empresaNombre;
    
    /**
     * Sucursal
     */
    private String sucursalNombre;
}