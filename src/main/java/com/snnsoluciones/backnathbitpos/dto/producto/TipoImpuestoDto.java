package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;

// DTO para respuesta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoImpuestoDto {
    private Long id;
    private String codigo;
    private String descripcion;
    private Boolean activo;
}
