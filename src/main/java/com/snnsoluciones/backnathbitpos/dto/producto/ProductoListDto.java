package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import java.math.BigDecimal;

// DTO simplificado para listados
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoListDto {
    private Long id;
    private String codigoInterno;
    private String codigoBarras;
    private String nombre;
    private String categoriaNombre;
    private BigDecimal precioVenta;
    private String monedaSimbolo;
    private Boolean esServicio;
    private Boolean activo;
}
