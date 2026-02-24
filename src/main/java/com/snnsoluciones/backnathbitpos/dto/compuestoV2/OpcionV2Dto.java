package com.snnsoluciones.backnathbitpos.dto.compuestoV2;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionV2Dto {
    private Long id;
    private String nombre;
    private Long productoId;
    private String productoNombre;
    private BigDecimal precioAdicional;
    private Boolean esDefault;
    private Boolean disponible;
    private Integer orden;
    private List<SlotV2Dto> subSlots;
}