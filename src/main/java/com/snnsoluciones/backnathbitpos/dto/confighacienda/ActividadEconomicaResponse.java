package com.snnsoluciones.backnathbitpos.dto.confighacienda;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActividadEconomicaResponse {
    private Long id;
    private String codigo;
    private String descripcion;
    private Boolean esPrincipal;
    private Integer orden;
}