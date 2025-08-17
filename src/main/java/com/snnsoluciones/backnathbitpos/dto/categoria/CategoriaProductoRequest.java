package com.snnsoluciones.backnathbitpos.dto.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaProductoRequest {
    
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
    
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;
    
    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    private String descripcion;
    
    @Size(max = 7, message = "El color debe ser un código hex válido")
    private String color; // Hex color como #FF5733
    
    @Size(max = 50, message = "El icono no puede exceder 50 caracteres")
    private String icono; // Nombre del icono o emoji
    
    private Integer orden;
}