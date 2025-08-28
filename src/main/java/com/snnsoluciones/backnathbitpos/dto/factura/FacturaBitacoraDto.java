package com.snnsoluciones.backnathbitpos.dto.factura;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO principal para la bitácora de facturación electrónica
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaBitacoraDto {
    private Long id;
    private Long facturaId;
    private String clave;
    private EstadoBitacora estado;
    private Integer intentos;
    private LocalDateTime proximoIntento;
    
    // Rutas S3
    private String xmlPath;
    private String xmlFirmadoPath;
    private String xmlRespuestaPath;
    
    // Info de Hacienda
    private String haciendaMensaje;
    
    // Tracking
    private String ultimoError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime procesadoAt;
    
    // Info adicional de la factura (para vistas)
    private String numeroFactura;
    private String nombreCliente;
    private Double montoTotal;
    private String empresaNombre;
    private String sucursalNombre;
}