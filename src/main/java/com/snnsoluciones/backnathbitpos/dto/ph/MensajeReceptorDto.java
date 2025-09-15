package com.snnsoluciones.backnathbitpos.dto.ph;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class MensajeReceptorDto {
    
    @NotBlank(message = "La clave del comprobante es requerida")
    @Size(min = 50, max = 50, message = "La clave debe tener exactamente 50 dígitos")
    @Pattern(regexp = "^[0-9]{50}$", message = "La clave debe contener solo números")
    private String clave;
    
    @NotBlank(message = "El número de cédula del emisor es requerido")
    @Size(max = 12, message = "El número de cédula no puede exceder 12 dígitos")
    private String numeroCedulaEmisor;
    
    @NotNull(message = "La fecha de emisión es requerida")
    private LocalDateTime fechaEmisionDoc;
    
    @NotNull(message = "El tipo de mensaje es requerido")
    @Pattern(regexp = "^[1-3]$", message = "El mensaje debe ser: 1=Aceptado, 2=Aceptado parcialmente, 3=Rechazado")
    private String mensaje;
    
    @Size(min = 5, max = 160, message = "El detalle debe tener entre 5 y 160 caracteres")
    private String detalleMensaje; // Obligatorio si mensaje != 1
    
    // Campos adicionales para el sistema
    private BigDecimal montoTotalImpuesto; // Para aceptación parcial
    private BigDecimal montoTotalImpuestoAcreditado; // IVA que se puede acreditar
    private Long compraId; // Referencia a la compra en nuestro sistema
    private Long terminalId;
    
    // Validación personalizada
    public boolean isDetalleRequerido() {
        return !"1".equals(mensaje) && (detalleMensaje == null || detalleMensaje.trim().isEmpty());
    }
}