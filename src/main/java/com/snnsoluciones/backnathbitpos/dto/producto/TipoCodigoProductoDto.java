package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para el TipoCodigoProducto (que faltaba)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoCodigoProductoDto {
    private Long id;
    private String codigo;
    private String descripcion;
    private Boolean activo;
}