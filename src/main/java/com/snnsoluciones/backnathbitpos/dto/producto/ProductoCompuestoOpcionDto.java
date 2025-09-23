package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoCompuestoOpcionDto {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private BigDecimal precioAdicional; // Puede ser positivo o negativo
    private Boolean esDefault;
    private Boolean disponible;
    private Integer orden;
}