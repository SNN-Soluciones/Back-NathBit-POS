// MensajeReceptorBitacoraDto.java
package com.snnsoluciones.backnathbitpos.dto.mensajereceptor;

import java.math.BigDecimal;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class MensajeReceptorBitacoraDto {
    private Long id;
    private Long compraId;
    private String clave;
    private String estado;
    private String tipoMensaje;
    private String tipoMensajeDescripcion;
    private String consecutivo;
    private Integer intentos;
    private LocalDateTime proximoIntento;
    private String ultimoError;
    private String haciendaMensaje;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Datos de la compra
    private String proveedorNombre;
    private String proveedorIdentificacion;
    private String numeroFactura;
    private BigDecimal montoTotal;
    private String empresaNombre;
    private String sucursalNombre;
    
    // URLs para descargar archivos
    private String urlDescargaXml;
    private String urlDescargaXmlFirmado;
    private String urlDescargaRespuesta;
    
    // Flags útiles
    private Boolean puedeReintentar;
    private Boolean puedeDescargar;
}