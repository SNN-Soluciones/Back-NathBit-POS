package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.util.List;

@Data
public class RecetaDTO {
    private Long productoId;
    private List<IngredienteDTO> ingredientes;
}