package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResponse {
    
    private Long id;
    private String clave; // Null para documentos internos
    private String consecutivo;
    private String tipoDocumento;
    private String tipoDocumentoNombre;
    private LocalDateTime fechaEmision;
    private String estado;
    
    // Cliente
    private Long clienteId;
    private String clienteNombre;
    private String clienteIdentificacion;
    
    // Montos
    private BigDecimal subtotal;
    private BigDecimal descuentos;
    private BigDecimal impuestos;
    private BigDecimal total;
    
    // Info adicional
    private String sucursalNombre;
    private String terminalNombre;
    private String cajeroNombre;
    
    // Para mostrar en UI
    private boolean puedeAnularse;
    private boolean puedeReenviarse;
    private boolean esElectronica;
    
    // Mensaje para el usuario
    private String mensaje; // "Factura creada exitosamente", etc.
}