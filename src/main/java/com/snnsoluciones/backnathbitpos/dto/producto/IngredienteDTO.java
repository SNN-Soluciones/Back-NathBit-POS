package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IngredienteDTO {
    private Long productoId;
    private BigDecimal cantidad;
}