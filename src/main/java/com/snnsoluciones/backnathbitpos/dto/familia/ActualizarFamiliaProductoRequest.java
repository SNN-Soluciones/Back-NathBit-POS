package com.snnsoluciones.backnathbitpos.dto.familia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar una FamiliaProducto existente
 * Similar a CrearFamiliaProductoRequest pero permite valores null
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarFamiliaProductoRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;
    
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String descripcion;
    
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    private String codigo;
    
    @Size(max = 20, message = "El color no puede exceder 20 caracteres")
    private String color;
    
    @Size(max = 50, message = "El icono no puede exceder 50 caracteres")
    private String icono;
    
    @NotNull(message = "El estado activa es obligatorio")
    private Boolean activa;
    
    @NotNull(message = "El orden es obligatorio")
    private Integer orden;
}