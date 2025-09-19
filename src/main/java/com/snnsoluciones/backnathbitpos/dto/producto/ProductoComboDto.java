// ProductoComboDto.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductoComboDto {
    private Long id;
    private Long productoId;
    private BigDecimal precioCombo;
    private BigDecimal ahorro;
    private String descripcionCombo;
    private List<ProductoComboItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}