package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FacturaRecepcionListResponse {
    
    private Long id;
    private String clave;
    private String numeroConsecutivo;
    private LocalDateTime fechaEmision;
    
    private String proveedorNombre;
    private String proveedorIdentificacion;
    
    private BigDecimal totalComprobante;
    private EstadoFacturaRecepcion estadoInterno;
    
    private Boolean mensajeReceptorEnviado;
    private Boolean convertidaACompra;
    private String estadoHacienda;
    private String mensajeHacienda;
}