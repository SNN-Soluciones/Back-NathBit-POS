package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductoCompuestoDto {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoCodigoInterno;
    private String instruccionesPersonalizacion;
    private Integer tiempoPreparacionExtra;
    private List<ProductoCompuestoSlotDto> slots = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}