// RecetaDto.java - Para la receta completa
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RecetaDto {
    private Long productoId;
    private String productoNombre;
    private List<RecetaIngredienteDto> ingredientes;
    private BigDecimal costoTotal;
    private BigDecimal margenGanancia;
    private Integer tiempoPreparacion; // en minutos
    private String instruccionesGenerales;
}