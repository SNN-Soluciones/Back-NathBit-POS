package com.snnsoluciones.backnathbitpos.dto.producto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Categoría de producto")
public class CategoriaProductoDto {

    @Schema(description = "ID de la categoría", example = "1")
    private Long id;

    @Schema(description = "Nombre de la categoría", example = "Bebidas")
    private String nombre;

    @Schema(description = "Color en formato hex", example = "#3498db")
    private String color;
}