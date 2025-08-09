package com.snnsoluciones.backnathbitpos.dto.empresa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResumenDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String nombreComercial;
    private String logo;
    private Boolean activa;
}