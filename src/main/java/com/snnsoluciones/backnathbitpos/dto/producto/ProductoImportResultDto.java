package com.snnsoluciones.backnathbitpos.dto.producto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para respuesta de importación
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImportResultDto {
    private Integer totalProcesados;
    private Integer exitosos;
    private Integer fallidos;
    private List<String> errores;
}
