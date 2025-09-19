// ProductoCompuestoSlotDto.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.util.List;

@Data
public class ProductoCompuestoSlotDto {
    private Long id;
    private String nombre; // "Tipo de Papa", "Proteína", "Salsa"
    private String descripcion;
    private Integer cantidadMinima;
    private Integer cantidadMaxima;
    private Boolean esRequerido;
    private Integer orden;
    private List<ProductoCompuestoOpcionDto> opciones;
}