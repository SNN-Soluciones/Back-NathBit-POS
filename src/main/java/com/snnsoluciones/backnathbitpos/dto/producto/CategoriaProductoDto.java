package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import jakarta.validation.constraints.*;

// DTO para respuesta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaProductoDto {
    private Long id;
    private Long empresaId;
    private String nombre;
    private String descripcion;
    private String color;
    private String icono;
    private Integer orden;
    private Long cantidadProductos;
    private Boolean activo;
}
