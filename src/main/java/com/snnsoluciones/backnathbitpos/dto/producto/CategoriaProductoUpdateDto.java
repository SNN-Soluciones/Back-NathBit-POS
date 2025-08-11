package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para actualizar
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaProductoUpdateDto {
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;
    
    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    private String descripcion;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un código hexadecimal válido")
    private String color;
    
    @Size(max = 50, message = "El icono no puede exceder 50 caracteres")
    private String icono;
    
    private Integer orden;
    private Boolean activo;
}