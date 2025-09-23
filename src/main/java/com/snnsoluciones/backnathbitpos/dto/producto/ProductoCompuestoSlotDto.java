package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductoCompuestoSlotDto {
    private Long id;
    private String nombre;
    private String descripcion;
    private Integer cantidadMinima;
    private Integer cantidadMaxima;
    private Boolean esRequerido;
    private Integer orden;
    private List<ProductoCompuestoOpcionDto> opciones = new ArrayList<>();
}