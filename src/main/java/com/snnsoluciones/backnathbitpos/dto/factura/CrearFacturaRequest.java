package com.snnsoluciones.backnathbitpos.dto.factura;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearFacturaRequest {
    
    @NotNull(message = "El cliente es requerido")
    private Long clienteId;
    
    @NotNull(message = "El tipo de documento es requerido")
    private TipoDocumento tipoDocumento;
    
    @NotBlank(message = "La condición de venta es requerida")
    private String condicionVenta; // 01=Contado, 02=Crédito
    
    private Integer plazoCredito; // Requerido si condicionVenta = 02
    
    @NotNull(message = "Los detalles son requeridos")
    @Size(min = 1, message = "Debe incluir al menos un producto")
    private List<DetalleFacturaRequest> detalles;
    
    @NotNull(message = "Los medios de pago son requeridos")
    @Size(min = 1, message = "Debe incluir al menos un medio de pago")
    private List<MedioPagoRequest> mediosPago;

    @NotNull(message = "Terminal es requerida")
    private Long terminalId;

    @NotNull(message = "Sesión de caja es requerida")
    private Long sesionCajaId;
    
    // Descuento global (opcional)
    private BigDecimal descuento = BigDecimal.ZERO;
    
    // Notas o comentarios (opcional)
    private String observaciones;
    
    // Datos adicionales para factura electrónica
    private String actividadEconomica; // Código de actividad si aplica
}