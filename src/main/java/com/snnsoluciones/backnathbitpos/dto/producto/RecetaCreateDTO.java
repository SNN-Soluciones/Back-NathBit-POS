package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.util.List;

@Data
public class RecetaCreateDTO {
    private Long empresaId;
    private Long productoId;
    private List<RecetaIngredienteDto> ingredientes;
}