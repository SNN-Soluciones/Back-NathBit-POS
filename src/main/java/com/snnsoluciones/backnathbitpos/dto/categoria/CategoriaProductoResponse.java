package com.snnsoluciones.backnathbitpos.dto.categoria;

import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaProductoResponse {
    
    private Long id;
    private Long empresaId;
    private Long sucursalId;
    private String empresaNombre;
    private String nombre;
    private String descripcion;
    private String color;
    private String icono;
    private Integer orden;
    private Boolean activo;
    private Long cantidadProductos; // Cantidad de productos en esta categoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}