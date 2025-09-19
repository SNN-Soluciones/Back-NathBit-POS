// RecetaIngredienteDto.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RecetaIngredienteDto {
    private Long id;
    private Long ingredienteId;
    private Long productoId;
    private String ingredienteNombre;
    private String ingredienteCodigo;
    private BigDecimal cantidad;
    private String unidadMedida;
    private BigDecimal factorConversion;
    private String notasPreparacion;
    private BigDecimal costoUnitario; // Para cálculo de costos
}