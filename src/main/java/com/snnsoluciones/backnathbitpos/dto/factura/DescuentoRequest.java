package com.snnsoluciones.backnathbitpos.dto.factura;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para descuentos por línea de detalle
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DescuentoRequest {
    
    /**
     * Código según nota 20 de Hacienda
     * 06 = Descuento promocional
     * 07 = Descuento comercial
     * 99 = Otros descuentos
     */
    @NotBlank(message = "El código de descuento es requerido")
    @Size(min = 2, max = 2, message = "El código debe tener 2 caracteres")
    @Pattern(regexp = "^(01|02|03|04|05|06|07|08|09|99)$", 
             message = "Código de descuento no válido")
    private String codigoDescuento;
    
    /**
     * Descripción cuando se usa código 99
     */
    @Size(min = 5, max = 100, message = "La descripción debe tener entre 5 y 100 caracteres")
    private String codigoDescuentoOTRO;
    
    /**
     * Naturaleza del descuento (para código 99)
     */
    @Size(min = 3, max = 80, message = "La naturaleza debe tener entre 3 y 80 caracteres")
    private String naturalezaDescuento;
    
    /**
     * Porcentaje de descuento (opcional si se envía monto)
     */
    @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede exceder 100%")
    @Digits(integer = 3, fraction = 2, message = "Formato de porcentaje inválido")
    private BigDecimal porcentaje;
    
    /**
     * Monto del descuento (opcional si se envía porcentaje)
     */
    @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
    @Digits(integer = 13, fraction = 5, message = "Formato de monto inválido")
    private BigDecimal montoDescuento;

    private Integer orden;
    
    /**
     * Validación: debe tener porcentaje O monto
     */
    public boolean isValid() {
        // Debe tener al menos uno
        if (porcentaje == null && montoDescuento == null) {
            return false;
        }
        
        // Si es código 99, validar campos adicionales
        if ("99".equals(codigoDescuento)) {
            return codigoDescuentoOTRO != null && 
                   codigoDescuentoOTRO.trim().length() >= 5 &&
                   naturalezaDescuento != null && 
                   naturalezaDescuento.trim().length() >= 3;
        }
        
        return true;
    }
}