package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImportResultDto {
    private Integer totalProcesados;
    private Integer exitosos;
    private Integer errores;

    @Builder.Default
    private List<ProductoImportDto> productosConError = new ArrayList<>();
    @Builder.Default
    private List<String> mensajesGenerales = new ArrayList<>();
}