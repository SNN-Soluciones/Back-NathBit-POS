// ProductoComboRequest.java - Para crear/actualizar combos
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductoComboRequest {
    
    @NotNull(message = "El precio del combo es requerido")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioCombo;
    
    private String descripcionCombo;
    
    @NotEmpty(message = "Debe incluir al menos un producto en el combo")
    @Size(min = 2, message = "Un combo debe tener al menos 2 productos")
    private List<ComboItemRequest> items;
    
    @Data
    public static class ComboItemRequest {
        @NotNull
        private Long productoId;
        
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal cantidad;
        
        private Integer orden;
    }
}