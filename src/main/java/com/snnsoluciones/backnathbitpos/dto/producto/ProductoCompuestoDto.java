// ProductoCompuestoDto.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductoCompuestoDto {
    private Long id;
    private Long productoId;
    private String instruccionesPersonalizacion;
    private Integer tiempoPreparacionExtra;
    private List<ProductoCompuestoSlotDto> slots;
    private LocalDateTime createdAt;
}