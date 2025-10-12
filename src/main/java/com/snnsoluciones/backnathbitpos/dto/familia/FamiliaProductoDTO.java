package com.snnsoluciones.backnathbitpos.dto.familia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para FamiliaProducto
 * Usado para devolver información de familias al cliente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamiliaProductoDTO {
    
    private Long id;
    private Long empresaId;
    private String nombre;
    private String descripcion;
    private String codigo;
    private String color;
    private String icono;
    private Boolean activa;
    private Integer orden;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Campo adicional útil para el frontend
    private Long cantidadProductos; // Cantidad de productos en esta familia
}