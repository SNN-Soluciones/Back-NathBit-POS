// ProductoCompuestoOpcionDto.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoCompuestoOpcionDto {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private BigDecimal precioAdicional;
    private Boolean esDefault;
    private Boolean disponible;
}