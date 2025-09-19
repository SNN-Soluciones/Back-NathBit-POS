// ProductoCompuestoRequest.java - Para crear/actualizar compuestos
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductoCompuestoRequest {
    
    private String instruccionesPersonalizacion;
    private Integer tiempoPreparacionExtra;
    
    @NotEmpty(message = "Debe definir al menos una categoría de personalización")
    private List<CompuestoSlotRequest> slots;
    
    @Data
    public static class CompuestoSlotRequest {
        @NotBlank
        private String nombre;
        
        private String descripcion;
        
        @Min(0)
        private Integer cantidadMinima = 1;
        
        @Min(1)
        private Integer cantidadMaxima = 1;
        
        private Boolean esRequerido = true;
        private Integer orden;
        
        @NotEmpty(message = "Debe incluir al menos una opción")
        private List<CompuestoOpcionRequest> opciones;
    }
    
    @Data
    public static class CompuestoOpcionRequest {
        @NotNull
        private Long productoId;
        
        @DecimalMin(value = "0.0")
        private BigDecimal precioAdicional = BigDecimal.ZERO;
        
        private Boolean esDefault = false;
        private Boolean disponible = true;
    }
}