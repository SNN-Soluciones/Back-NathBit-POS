package com.snnsoluciones.backnathbitpos.dto.compuestoV2;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCompuestoV2Dto {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String instruccionesPersonalizacion;
    private List<SlotV2Dto> slots;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}