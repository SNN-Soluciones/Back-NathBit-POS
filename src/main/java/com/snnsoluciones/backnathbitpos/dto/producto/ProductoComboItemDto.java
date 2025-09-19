// ProductoComboItemDto.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoComboItemDto {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private BigDecimal cantidad;
    private BigDecimal precioUnitarioReferencia;
    private Integer orden;
}