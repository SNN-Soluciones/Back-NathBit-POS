package com.snnsoluciones.backnathbitpos.dto.cabys;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CABySDto {
    private Long id;
    private String codigo;
    private String descripcion;
    private String impuestoSugerido;
    private Boolean activo;
}