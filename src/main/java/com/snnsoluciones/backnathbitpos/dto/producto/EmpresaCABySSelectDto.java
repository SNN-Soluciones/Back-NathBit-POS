package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO simplificado para selects
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCABySSelectDto {
    private Long id;
    private String codigo;
    private String descripcion;
    private String descripcionPersonalizada;
}