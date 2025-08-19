package com.snnsoluciones.backnathbitpos.dto.factura;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para crear Otros Cargos en una factura
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtroCargoRequest {
    
    /**
     * Código según nota 16 de Hacienda
     * 06 = Impuesto servicio 10%
     */
    @NotBlank(message = "El tipo de documento es requerido")
    @Size(min = 2, max = 2, message = "El tipo debe tener 2 caracteres")
    @Pattern(regexp = "^(01|02|03|04|05|06|07|08|09|10|99)$", 
             message = "Código de tipo no válido")
    private String tipoDocumentoOC;
    
    /**
     * Descripción cuando se usa código 99
     */
    @Size(min = 5, max = 100, message = "La descripción debe tener entre 5 y 100 caracteres")
    private String tipoDocumentoOTROS;
    
    /**
     * Nombre descriptivo del cargo
     */
    @NotBlank(message = "El nombre del cargo es requerido")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombreCargo;
    
    /**
     * Porcentaje del cargo (opcional)
     */
    @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede exceder 100%")
    @Digits(integer = 3, fraction = 2, message = "Formato de porcentaje inválido")
    private BigDecimal porcentaje;
    
    /**
     * Monto total del cargo
     */
    @NotNull(message = "El monto del cargo es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Digits(integer = 13, fraction = 5, message = "Formato de monto inválido")
    private BigDecimal montoCargo;
    
    // Campos para terceros (cuando tipoDocumentoOC = '04')
    private String terceroTipoIdentificacion;
    
    @Size(max = 20, message = "La identificación no puede exceder 20 caracteres")
    private String terceroNumeroIdentificacion;
    
    @Size(max = 100, message = "El nombre del tercero no puede exceder 100 caracteres")
    private String terceroNombre;
    
    /**
     * Validación cruzada
     */
    public boolean isValid() {
        // Si es código 99, debe tener descripción
        if ("99".equals(tipoDocumentoOC)) {
            return tipoDocumentoOTROS != null && tipoDocumentoOTROS.trim().length() >= 5;
        }
        
        // Si es código 04, debe tener datos del tercero
        if ("04".equals(tipoDocumentoOC)) {
            return terceroNumeroIdentificacion != null && terceroNombre != null;
        }
        
        return true;
    }
}