package com.snnsoluciones.backnathbitpos.dto.familia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una nueva FamiliaProducto
 * Contiene validaciones Jakarta Bean Validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearFamiliaProductoRequest {
    
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
    @Builder.Default
    private Boolean activa = true;
    
    @NotNull(message = "El orden es obligatorio")
    @Builder.Default
    private Integer orden = 0;
}