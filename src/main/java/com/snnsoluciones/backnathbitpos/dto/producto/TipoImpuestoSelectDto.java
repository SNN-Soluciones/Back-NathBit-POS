package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO simplificado
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoImpuestoSelectDto {
    private Long id;
    private String codigo;
    private String descripcion;
}