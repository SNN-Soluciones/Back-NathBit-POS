package com.snnsoluciones.backnathbitpos.dto.producto;

import java.math.BigDecimal;
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
    private Boolean usaFamilia;
    private Long familiaId;
    private String familiaNombre;
    private String familiaCodigo;
    private String familiaColor;
    private BigDecimal precioAdicionalPorOpcion;
    private List<ProductoCompuestoOpcionDto> opciones = new ArrayList<>();
}